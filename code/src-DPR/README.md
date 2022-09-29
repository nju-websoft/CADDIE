# DPR

If you want to reuse the dense passage retrieval (DPR) model in our paper, you should firstly follow the preprocess steps to generate the pseudo documents for all datasets. Then you may configure and apply the model to your queries and datasets. 

## Preprocess

For the preprocess, please: 

1. Use [IlluSnip](https://github.com/nju-websoft/BANDAR) to extract top-$k$ triples for each RDF dataset, and store the results in a local MySQL database table. Note to configure the paths and database to your local settings. 
2. Run [create_pseudo_document.py](https://github.com/nju-websoft/CADDIE/blob/main/code/src-DPR/create_pseudo_document.py) to create the two pseudo documents for each RDF dataset. 

## Use DPR

We use the implementation of [Karpukhin et al., 2020](https://github.com/facebookresearch/DPR). To obtain the model, you can execute the following command:

```
git clone https://github.com/facebookresearch/DPR.git
```

Then you should: 

1. Configure the paths of pseudo documents and queries to your local settings in [conf/ctx_sources/](https://github.com/nju-websoft/CADDIE/tree/main/code/src-DPR/conf/ctx_sources) and [conf/datasets/](https://github.com/nju-websoft/CADDIE/tree/main/code/src-DPR/conf/datasets). We use [ACORDAR](https://github.com/nju-websoft/ACORDAR) queries in our experiments.
2. Follow the instructions provided in the original [README](https://github.com/facebookresearch/DPR/blob/main/README.md) file to use DPR. We also provide a integrated script [pipeline.sh](https://github.com/nju-websoft/CADDIE/blob/main/code/src-DPR/scripts/pipeline.sh) for this process. 
   - To run the script, you should configure the paths to your local settings. For example, modify `$base_path/dpr/downloads/checkpoint/retriever/single-adv-hn/nq/bert-base-encoder.cp` to your local path of the BERT encoder checkpoint. 


