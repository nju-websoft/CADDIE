# CADDIE

Data, source codes and experimental results for paper "*Metadata Is Not Enough: Content-Based Ad Hoc RDF Dataset Retrieval*". 

> The rapid growth of open and structured RDF data on the Web has promoted the development of dataset search as an important research topic. The core function of existing systems is ad hoc dataset retrieval (AHDR) based on the metadata of datasets, which contains limited information and often suffers from quality issues. To overcome the limitations, in this paper, we systematically investigate content-based AHDR to exploit the actual RDF data in datasets. We address three main tasks of content-based AHDR with novel methods for handling the large size and complex structure of RDF data to facilitate dataset retrieval, deduplication, and snippet extraction. These methods are integrated into an online and open-source prototype called CADDIE. The effectiveness and feasibility of its components are evaluated on a public test collection and by a user study.

## Datasets

- All the datasets used in our system are provided in [datasets.zip](https://github.com/nju-websoft/CADDIE/blob/main/datasets.zip). It contains the metadata of each dataset, including its title, download link, updated time, etc. 
- Each dataset has a unique numerical *"dataset_id"*. Note that a dataset may have multiple dump files, identified by different *"file_id"*.

## Source Codes and Dependencies

### Dependencies

- JDK 8+
- MySQL 5.6+
- Apache Lucene 7.5.0
- Apache Jena 3.9.0

All useful packages (jar files) are provided in [code/lib](https://github.com/nju-websoft/CADDIE/tree/main/code/lib). 

### Database Settings

- Caddie is built upon a local MySQL database. 
- [code/database_tables.sql](https://github.com/nju-websoft/CADDIE/blob/main/code/database_tables.sql) provides the schema of the database. One can restore our database with empty tables by running this SQL file. Table *dataset_summary* stores the metadata of all the datasets. Table *triple* stores all the RDF triples parsed from the datasets. Table *rdf_term* stores all the terms (i.e., IRIs, blank nodes, literals) appearing in the datasets. 
- In all the tables, *"dataset_id"* and *"file_id"* refer to the above-mentioned IDs in [datasets.zip](https://github.com/nju-websoft/CADDIE/blob/main/datasets.zip).

### Codes

- The source codes of Caddie are provided in [code/src](https://github.com/nju-websoft/CADDIE/tree/main/code/src). 
- To restore our system, please import [src](https://github.com/nju-websoft/CADDIE/tree/main/code/src) into a java project. Then import all the packages provided in [code/lib](https://github.com/nju-websoft/CADDIE/tree/main/code/lib). 

Here we briefly explain the structure of source codes.

#### For Content-Based Dataset Deduplication:

- The package [MSGLabeling](https://github.com/nju-websoft/CADDIE/tree/main/code/src/MSGLabeling) handles the data hashing process. It firstly reads each dataset from the local database, splits them into minimum self-contained graphs (MSGs), computes hash values for each MSG, and stores the hash values into the table "triple" in the database.
- To compute the hash values for each MSG, we reused the [BLABEL](http://blabel.github.io/) algorithm.
- The package [deduplication](https://github.com/nju-websoft/CADDIE/tree/main/code/src/deduplication) implements the similarity join method PPJoin+ to identify pairs of similar datasets. Then the computed similarity values are also indexed as local files. 

#### For Content-Based Dataset Retrieval Model:

- The package [Retrieval](https://github.com/nju-websoft/CADDIE/tree/main/code/src/Retrieval) includes the codes for indexing and retrieving datasets. The weights of fields in the inverted index can be modified in [GlobalVariances](https://github.com/nju-websoft/CADDIE/blob/main/code/src/Retrieval/GlobalVariances.java). 
- The package [ReRanking](https://github.com/nju-websoft/CADDIE/tree/main/code/src/ReRanking) contains the codes for diversity-based re-ranking based on maximal marginal relevance (MMR).
- The dense passage retrieval (DPR) model we used in our experiments is provided in [code/src-DPR](https://github.com/nju-websoft/CADDIE/tree/main/code/src-DPR). Usages and details are provided in [code/src-DPR/README](https://github.com/nju-websoft/CADDIE/blob/main/code/src-DPR/README.md). 

#### For Content-Based Dataset Snippet Extraction:

- The package [QPCSG](https://github.com/nju-websoft/CADDIE/tree/main/code/src/QPCSG) contains the codes for data sample extraction. Its main entrance is in [GetResultTree](https://github.com/nju-websoft/CADDIE/blob/main/code/src/QPCSG/GetResultTree.java). Besides, as a preprocessing step, it also requires some distance indexes to be built as in [HubLabelIndexer](https://github.com/nju-websoft/CADDIE/blob/main/code/src/QPCSG/HubLabelIndexer.java). 

The rest packages (not mentioned above) are mainly used to support these components or support experiments. 

## Experiments

Data and experimental results are provided in [experiment](https://github.com/nju-websoft/CADDIE/tree/main/experiment).

#### Evaluation of Content-Based Dataset Retrieval Model (T1)

- The test collection we used to evaluate Caddie is from [ACORDAR](https://github.com/nju-websoft/ACORDAR). 
- [experiment/1-retrieval](https://github.com/nju-websoft/CADDIE/tree/main/experiment/1-retrieval) provides all the retrieval results on the test collection. We follow the original format (as in [ACORDAR](https://github.com/nju-websoft/ACORDAR)) to publish results. Each row represents a query-dataset pair. The first column represents query id. The third column stands for dataset id. The fourth column is the ranking score computed by the retrieval model. 
- Following the evaluation method used by the test collection, we also compute the NDCG and MAP scores using [trec_eval](https://trec.nist.gov/trec_eval/).
- [experiment/1-retrieval/validation-set](https://github.com/nju-websoft/CADDIE/tree/main/experiment/1-retrieval/validation-set) provides the validation set we used to tune field weights of our retrieval model. The file [query](https://github.com/nju-websoft/CADDIE/blob/main/experiment/1-retrieval/validation-set/query.txt) provides the keyword queries used in the validation set, each with a numerical ID. Each row in [annotation](https://github.com/nju-websoft/CADDIE/blob/main/experiment/1-retrieval/validation-set/annotation.txt) represents a query-dataset-relevance tuple, where the queries are represented by their IDs as in [query](https://github.com/nju-websoft/CADDIE/blob/main/experiment/1-retrieval/validation-set/query.txt), the datasets are represented by their *"dataset_id"*, and the relevance scores are annotated in a 0--2 scale (0: irrelevant, 1: partially relevant, 2: highly relevant).

#### Evaluation of Content-Based Dataset Deduplication (T2)

- [experiment/2-deduplication](https://github.com/nju-websoft/CADDIE/tree/main/experiment/2-deduplication) provides all the identified dataset pairs whose metadata or data similarity exceeds a threshold. [dataSimOver90](https://github.com/nju-websoft/CADDIE/blob/main/experiment/2-deduplication/dataSimOver90.txt) consists of all the dataset pairs with data similarity $>0.9$. [dataSimOver90_metaSimBelow90](https://github.com/nju-websoft/CADDIE/blob/main/experiment/2-deduplication/dataSimOver90_metaSimBelow90.txt) includes the dataset pairs with data similarity $>0.9$ and metadata similarity $<0.9$. [metaSimOver90](https://github.com/nju-websoft/CADDIE/blob/main/experiment/2-deduplication/metaSimOver90.txt) contains the pairs with metadata similarity $>0.9$. [metaSimOver90_dataSimBelow90](https://github.com/nju-websoft/CADDIE/blob/main/experiment/2-deduplication/metaSimOver90_dataSimBelow90.txt) provides the pairs with metadata similarity $>0.9$ and data similarity $<0.9$. 
- In the result files, each row represents a pair of datasets identified by their *"dataset_id"*. 

#### Evaluation of Content-Based Dataset Snippet Extraction (T3)

- [experiment/3-snippet](https://github.com/nju-websoft/CADDIE/tree/main/experiment/3-snippet) provides all the original relevance judgments and ratings from the participants of our user study. 
- In [user-study-results](https://github.com/nju-websoft/CADDIE/blob/main/experiment/3-snippet/user-study-results.xlsx), each row represents a record. The column *"user"* stands for the user id. The *"dataset_relevance"* is the relevance score judged by the user. The *"accuracy"* is a binary score comparing the user-judged relevance with the gold standard from the test collection. The *"rating_metadata"* and *"rating_snippet"* are the ratings to the usefulness of metadata and snippets, respectively. 

> If you have any question about the codes or experimental results, please email to [xxwang@smail.nju.edu.cn](mailto:xxwang@smail.nju.edu.cn).

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/nju-websoft/CADDIE/blob/main/LICENSE) for details. 

## Citation

If you use these data or codes, please kindly cite it as follows:

```
@article{caddie,
  author    = {Xiaxia Wang and Qing Shi and Qiaosheng Chen and Weiqing Luo and Tengteng Lin and Jeff Z. Pan and Baifan Zhou and Evgeny Kharlamov and Gong Cheng},
  title     = {Metadata Is Not Enough: Content-Based Ad Hoc RDF Dataset Retrieval},
  year      = {2023}
}
```
