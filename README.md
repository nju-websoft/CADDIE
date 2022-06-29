# CBDS
Data, source codes and experimental results for paper "*[From Metadata to Content-Based Dataset Search](https://github.com/nju-websoft/CBDS)*". 

> The rapid growth of open data published on the Web has promoted fruitful research and development of dataset search systems. Current implementations mainly exploit the metadata of datasets to support ad hoc dataset retrieval. However, metadata contains limited information and often suffers from quality issues. Therefore, Content-Based Dataset Search (CBDS) is demanded to exploit the actual data to provide users with search results featuring higher relevance, lower redundancy, and better explainability. In this vision paper, we systematically introduce CBDS and identify its advantages, research tasks, and challenges. We ground our vision in Caddie, a prototype of content-based ad hoc dataset retrieval over RDF datasets, and we empirically demonstrate its effectiveness and feasibility. Finally, we discuss potential impacts of CBDS to users, researchers, developers, and data providers.

## Datasets

- All the datasets used in our system are provided in [datasets.zip](https://github.com/nju-websoft/CBDS/blob/main/datasets.zip). It contains all the metadata of each dataset, including the title, download link, updated time, etc. 
- Each dataset has a unique numerical ID which are used in the experiments.

## Source Codes and Dependencies

### Environments

- JDK 8+
- MySQL 5.6+

useful packages (jar files) are all provided in [code/lib](https://github.com/nju-websoft/CBDS/tree/main/code/lib). 

### Codes

TBD

## Experiments

The resources and experimental results are provided in [experiment](https://github.com/nju-websoft/CBDS/tree/main/experiment).

### Evaluation of Content-Based Dataset Retrieval Model (T1)


#### Evaluation of Content-Based Dataset Deduplication (T2)


##### Evaluation of Content-Based Dataset Snippet Extraction (T3)


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
