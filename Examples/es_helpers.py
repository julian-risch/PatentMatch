#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Aug 28 13:45:09 2017

@author: Samuele Garda
"""
import json
import glob
import logging
import argparse
import requests
import elasticsearch 
from elasticsearch import Elasticsearch,helpers
from datetime import datetime

logger = logging.getLogger(__name__)
logging.basicConfig(format = '%(asctime)s : %(levelname)s : %(module)s: %(message)s', level = 'INFO')

def parse_arguments():
  """
  Parse options for functions.
  """
  parser = argparse.ArgumentParser(description='Tool for managing Elasticsearch indices')
  subparsers = parser.add_subparsers()
  
  create = subparsers.add_parser('create', help = 'Create Elasticsearch index')
  create.add_argument('-i','--index', required = True,help = 'Name of the new index')
  create.add_argument('-m','--mappings',type=argparse.FileType('r'),required = True,help = 'File where mappings configuration is store for the index')
  
  delete = subparsers.add_parser('delete', help = 'Delete one or more Elasticsearch indices')
  delete.add_argument('-e','--erase', nargs = '*', required = True,help = 'Name of index to delete')
  
  index = subparsers.add_parser('index', help = 'Index JSON files')
  index.add_argument('-d','--dir',required = True, help = 'Directory where the JSON files are stored. Be sure that in this path to the Spider folder!!!')
  index.add_argument('-l','--location',required = True, help = 'Name of the index where to index JSON files. If index do not exist it is created' )
  index.add_argument('-t','--item-type',required = True, help = 'Name of type to be stored in ES index' )
  index.add_argument('-c','--chunk-size', type = int, nargs='?', default = 500 , help ='Number of JSON line to load into memory before indexing')
  
  reindex = subparsers.add_parser('reindex', help='Reindex index')
  reindex.add_argument('-s','--source', required = True, help='Source index where documents are stored')
  reindex.add_argument('-t','--target',required = True, help = 'Target index where to move documents')
  
  args = parser.parse_args()
  
  return args


def create_index(es,index_name, body):
  if not es.indices.exists(index_name):
    es.indices.create(index = index_name, body = body)

def delete_indices(es,indices_name):

  for index in indices_name:
    if es.indices.exists(index):
      es.indices.delete(index = index)
    else:
      logger.info('Index `{}` not found'.format(index))

def reindex(es,source_index, target_index):
  helpers.reindex(es, source_index = source_index, target_index = target_index )
  
def lazy_indexing(es,path,chunck,index,item_type):
  
  def serialize_json(json_line):
    
    to_null = ['author', 'article_tag','list_of_tags','keywords','news_keywords']
    
    for tag in to_null:
      if json_line[tag] == '---':
        json_line[tag] = None
    
    if json_line['publication_date'] == '---':
      json_line['publication_date'] = datetime.strptime('1900-01-01','%Y-%m-%d')
    else:
      try:
        json_line['publication_date'] = datetime.strptime(json_line['publication_date'], '%d %B %Y').date()
      except ValueError:
        try:
          json_line['publication_date'] = datetime.strptime(json_line['publication_date'].replace('T',' '), '%Y-%m-%d %H:%S')
        except ValueError:
          pass
      
    return json_line
  
  def lazy_json_load(filename):
    with open(filename) as infile:
      for line in infile:
        json_line = json.loads(line)
        formattd_json_line = serialize_json(json_line)
        index_action = {
              '_index': index,
              '_type': item_type,
              '_id' : formattd_json_line['url'],
              '_source': formattd_json_line
          }
        yield index_action
        
  files = [file for file in glob.glob(path + '/**/*.json', recursive=True) if not "active.json" in file.split('/')]
  
  logger.info("Fond {0} documents to index".format(len(files)))
  
  for filename in files:
    logger.info("Indexing : {}".format(filename))
    helpers.bulk(client = es,chunk_size  = chunck, actions=lazy_json_load(filename), index= index,doc_type='news_article', stats_only = True)

if __name__ == "__main__":
  
  elasticsearch.connection.http_urllib3.warnings.filterwarnings('ignore')
  requests.packages.urllib3.disable_warnings()
 
  args = parse_arguments()
  
#  ES = Elasticsearch(hosts='localhost​',verify_certs=False,use_ssl = True, http_auth= "admin:admin")
  
  ES = Elasticsearch(['localhost'])

  try:
    if args.index:
      create_index(es = ES, index_name = args.index, body = args.mappings.read())
  except AttributeError:
    pass
  
  try:
    if args.erase:
      delete_indices(es = ES, indices_name = args.erase)
  except AttributeError:
    pass

  try:
    if args.source:
      reindex(es = ES, source_index= args.source , target_index= args.target)
  except AttributeError:
    pass
  
  try:
    if args.dir:
     lazy_indexing(es = ES, path = args.dir, index = args.location, item_type= args.item_type, chunck = args.chunk_size)
  except AttributeError:
    pass
      


