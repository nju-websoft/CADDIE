# CBDS

Data, source codes and experimental results for paper "*[From Metadata to Content-Based Dataset Search](https://github.com/nju-websoft/CBDS)*". 

> The rapid growth of open data published on the Web has promoted fruitful research and development of dataset search systems. Current implementations mainly exploit the metadata of datasets to support ad hoc dataset retrieval. However, metadata contains limited information and often suffers from quality issues. Therefore, Content-Based Dataset Search (CBDS) is demanded to exploit the actual data to provide users with search results featuring higher relevance, lower redundancy, and better explainability. In this vision paper, we systematically introduce CBDS and identify its advantages, research tasks, and challenges. We ground our vision in Caddie, a prototype of content-based ad hoc dataset retrieval over RDF datasets, and we empirically demonstrate its effectiveness and feasibility. Finally, we discuss potential impacts of CBDS to users, researchers, developers, and data providers.

## Datasets

- All the datasets used in our system are provided in [datasets.zip](https://github.com/nju-websoft/CBDS/blob/main/datasets.zip). It contains all the metadata of each dataset, including the title, download link, updated time, etc. 
- Each dataset has a unique numerical *"dataset_id"* which are used in the experiments. Note that one dataset could have multiple corresponding dump files, identified by *"file_id"*.

## Source Codes and Dependencies

### Environments

- JDK 8+
- MySQL 5.6+

useful packages (jar files) are all provided in [code/lib](https://github.com/nju-websoft/CBDS/tree/main/code/lib). 

### Database Settings

- Caddie is built upon a local MySQL database. 
- [code/database_tables.sql](code/database_tables.sql) provides the structure of database tables used in our system. One can import empty tables directly by running the .sql file. Table *dataset_metadata* stores all the metadata of the datasets. Table *triple* stores all the parsed RDF triples. Table *rdf_term* stores all the terms (i.e., IRIs, blank nodes, literals) used in the datasets. 
- For all the database tables, the fields *"dataset_id"* and *"file_id"* are correspond to the IDs appeared in [datasets.zip](https://github.com/nju-websoft/CBDS/blob/main/datasets.zip).

### Codes

TBD

## Experiments

The resources and experimental results are provided in [experiment](https://github.com/nju-websoft/CBDS/tree/main/experiment).

#### Evaluation of Content-Based Dataset Retrieval Model (T1)

- The test collection we used to evaluate Caddie is from [ACORDAR](https://github.com/nju-websoft/ACORDAR). 
- [experiment/1-retrieval](experiment/1-retrieval) provides all the retrieval results on the test collection. We follow the original format (as in [ACORDAR](https://github.com/nju-websoft/ACORDAR)) to present the results. Each row represents a query-dataset pair. The first column represents the query id. The third column stands for dataset id. The fourth column is the ranking score returned by the retrieval system. 
- Following the evaluation of the test collection, we also compute the NDCG and MAP scores using [trec_eval](https://trec.nist.gov/trec_eval/) tool.

#### Evaluation of Content-Based Dataset Deduplication (T2)

- [experiment/2-deduplication](experiment/2-deduplication) provides all the identified dataset pairs whose metadata or data similarity exceeds the limit. [dataSimOver90](https://github.com/nju-websoft/CBDS/blob/main/experiment/2-deduplication/dataSimOver90.txt) consists of all the dataset pairs with data similarity $> 0.9$. [dataSimOver90_metaSimBelow90](https://github.com/nju-websoft/CBDS/blob/main/experiment/2-deduplication/dataSimOver90_metaSimBelow90.txt) includes the dataset pairs with data similarity and metadata similarity $< 0.9$. [metaSimOver90](https://github.com/nju-websoft/CBDS/blob/main/experiment/2-deduplication/metaSimOver90.txt) contains the pairs with metadata similarity $> 0.9$. [metaSimOver90_dataSimBelow90](https://github.com/nju-websoft/CBDS/blob/main/experiment/2-deduplication/metaSimOver90_dataSimBelow90.txt) contains the pairs with metadata similarity and data similarity $< 0.9$. 
- In the result files, each row represents a pair of datasets identified by their *"dataset_id"*. 

#### Evaluation of Content-Based Dataset Snippet Extraction (T3)


## License

 This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/nju-websoft/CBDS/blob/main/LICENSE) for details. 

## Citation

If you use these data or codes, please kindly cite it as follows:

```latex
@inproceedings{caddie,
  author    = {Xiaxia Wang and Qing Shi and Jeff Z. Pan and Tengteng Lin and Qiaosheng Chen and Baifan Zhou and Evgeny Kharlamov and Wei Hu and Gong Cheng},
  title     = {From Metadata to Content-Based Dataset Search},
  year      = {2022}
}
```
