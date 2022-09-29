#!/bin/bash 
base_path="your_dpr_repo_path"
output_dir_name="your_output_directory_name"

data_type="both"
data_type1="metadata"
data_type2="content"

embedding_name="embedding_0"

ctx_source1="dataset_ctx_${data_type1}"
ctx_source2="dataset_ctx_${data_type2}"

QUERY_SOURCE="your_query_source_name"

pretrain_file_path="$base_path/dpr/downloads/checkpoint/retriever/single-adv-hn/nq/bert-base-encoder.cp"

embedding_path1="$base_path/$output_dir_name/embeddings/${data_type1}/${embedding_name}"
embedding_path2="$base_path/$output_dir_name/embeddings/${data_type2}/${embedding_name}"

res_name="res_0.json"

retrieve_res_path="$base_path/$output_dir_name/retrieve_results/${data_type}/${res_name}"

batch_size=8
lr=2e-5
train_name="finetune_bs${batch_size}"
train_path="$base_path/$output_dir_name/checkpoints/${train_name}"

embedding(){
	CUDA_VISIBLE_DEVICES=4 python generate_dense_embeddings.py \
	model_file=$model_file_path \
	ctx_src=$1 \
	out_file=$2 \
	insert_title=False
}

retrieve(){
	CUDA_VISIBLE_DEVICES=4 python dense_retriever.py \
	model_file=$model_file_path \
	qa_dataset=${QUERY_SOURCE} \
	ctx_datatsets=[$ctx_source1,$ctx_source2] \
	encoded_ctx_files=[\"${embedding_path1}\",\"${embedding_path2}\"] \
	out_file=$retrieve_res_path
}

train(){
	CUDA_VISIBLE_DEVICES=0 \
	python train_dense_encoder.py \
	train_datasets=[ds_0,ds_1,ds_2] \
	dev_datasets=[ds_3] \
	train=biencoder_local \
	output_dir=$train_path \
	model_file=$pretrain_file_path \
	my_batch_size=$batch_size \
	my_learning_rate=$lr \
	ignore_checkpoint_optimizer=true
}

if [ $1 == "embedding" ]; then
    embedding $ctx_source1 $embedding_path1
	embedding $ctx_source2 $embedding_path2

elif [ $1 == "retrieve" ]; then
	echo "===================== retrieve start ====================="
	retrieve

elif [ $1 == "train" ]; then
    echo "===================== train start ====================="
	train
	

elif [ $1 == "pipeline" ]; then
	echo "===================== train start ====================="
	train
    echo "===================== embedding start ====================="
	embedding $ctx_source1 $embedding_path1
	embedding $ctx_source2 $embedding_path2
	echo "===================== retrieve start ====================="
	retrieve


else
    echo "$1: not supported"

fi
