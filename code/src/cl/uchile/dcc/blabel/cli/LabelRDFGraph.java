package cl.uchile.dcc.blabel.cli;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;
import cl.uchile.dcc.blabel.label.util.GraphLabelIterator;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.cli.*;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.FlyweightNodeIterator;

import java.io.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Main method for leaning and/or labelling an RDF HubLabel.graph.
 * 
 * @author ahogan
 *
 */
public class LabelRDFGraph {
	static final Logger LOG = Logger.getLogger(LabelRDFGraph.class.getSimpleName());
	public static final Level LOG_LEVEL = Level.INFO;
	static{
		for(Handler h : LOG.getParent().getHandlers()){
		    if(h instanceof ConsoleHandler){
		        h.setLevel(LOG_LEVEL);
		    }
		} 
		LOG.setLevel(LOG_LEVEL);
	}

	public static int FW = 100000;

	public static String STD = "std";

	public static String DEFAULT_ENCODING = "UTF-8";

	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		long b4 = System.currentTimeMillis();

		// -i: input file [enter '"+STD+"' for stdin]
		// -s: 0:md5 1:murmur3_128 2:sha1 3:sha256 4:sha512
		// -o: output file [enter '"+STD+"' for stdout]

		HashFunction hf = Hashing.md5(); //.murmur3_128(); .sha1(); .sha256(); .sha512()

		InputStream is = new FileInputStream("C:/Users/Desktop/1.nt");
		String iestr = DEFAULT_ENCODING;

		BufferedReader br = new BufferedReader(new InputStreamReader(is,iestr));
		NxParser nxp = new NxParser(br);

		if(!nxp.hasNext()){
			LOG.info("Empty input");
			return;
		}

		OutputStream os = new FileOutputStream("C:/Users/Desktop/11.nt");
		String oestr = DEFAULT_ENCODING;

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os,oestr));
		CallbackNxBufferedWriter cb = new CallbackNxBufferedWriter(bw);

		// re-use node references: saves mem
		// at cost of map lookups
		Iterator<Node[]> iter = nxp;
		iter = new FlyweightNodeIterator(FW,iter);
		
		// load the HubLabel.graph into memory
		Collection<Node[]> stmts = new ArrayList<Node[]>();
		boolean bnode = false;
		while(iter.hasNext()){
			Node[] triple = iter.next();
			if(triple.length>=3){
				stmts.add(new Node[]{triple[0], triple[1], triple[2]});
				bnode = bnode | (triple[0] instanceof BNode) | (triple[2] instanceof BNode);
			} else{
				LOG.warning("Not a triple "+Nodes.toN3(triple));
			}
		}
		LOG.info("Loaded "+stmts.size()+" triples");
		
		if(!bnode){
			LOG.info("No bnodes ... buffering triple input to output");
			for(Node[] triple:stmts){
				cb.processStatement(triple);
			}
		}
		
		if(bnode){
			LOG.info("Starting leaning ...");
			GraphLeaningResult glr  = leanGraph(stmts,cb);
			if(glr.getLeanData().size() == stmts.size()){
				LOG.info("Input HubLabel.graph was lean");
			} else{
				LOG.info("Removed "+(stmts.size() - glr.getLeanData().size())+" triples during leaning");
			}

			stmts = glr.getLeanData();

			LOG.info("Starting labelling ...");
			// set the arguments for the labelling
			GraphLabellingArgs cla = new GraphLabellingArgs();
			if(hf!=null)
				cla.setHashFunction(hf);

			boolean writeBnode = true; // true: the bnodes in the output starts with "_:", false: bnodes are output as <...>

			labelGraph(stmts,cb,cla,"",writeBnode); // IN THIS STEP: LOG Graph Hash
			LOG.info("... done.");
		}

		LOG.info("Finished in "+(System.currentTimeMillis()-b4)+" ms");
		br.close();
		bw.close();
	}

	private static GraphLeaningResult leanGraph(Collection<Node[]> data, CallbackNxBufferedWriter cb) throws InterruptedException {
		DFSGraphLeaning dfs = new DFSGraphLeaning(data);
		LOG.info("Running leaning ...");
		GraphLeaningResult glr = dfs.call();
		LOG.info("... done.");
		
		LOG.info("Number of input bnodes "+glr.getCoreMap().size());
		LOG.info("Depth "+glr.getDepth());
		LOG.info("Number of joins "+glr.getJoins());
		LOG.info("Core endomorphism (witness mapping) "+glr.getCoreMap());
		return glr;
	}

	/**
	 * Labels the input HubLabel.graph and writes the result to the callback.
	 * 
	 * @param in - The input data in Nx format
	 * @param out - The output data in Nx format
	 * @param cla - The options for running the labelling
	 * @param prefix - Any prefix to be prepended to the label (e.g., a skolem prefix)
	 * @param writeBnode - Writes bnodes if true, otherwise writes URIs
	 * @throws HashCollisionException 
	 * @throws InterruptedException 
	 * 
	 * @returns null if no blank nodes in HubLabel.graph, otherwise returns an object with the details of the colouring process (including, e.g., a unique hash)
	 */
	public static final GraphLabellingResult labelGraph(Collection<Node[]> stmts, Callback out, GraphLabellingArgs cla, String prefix, boolean writeBnode) throws InterruptedException, HashCollisionException{
		// create a new labeler
		GraphLabelling cl = new GraphLabelling(stmts,cla);

		LOG.info("Running labelling ...");
		GraphLabellingResult clr = cl.call();
		LOG.info("... done.");

		LOG.info("Number of blank nodes: "+clr.getBnodeCount());
		LOG.info("Number of partitions: "+clr.getPartitionCount());
		LOG.info("Number of colour iterations: "+clr.getColourIterationCount());
		LOG.info("Number of leafs: "+clr.getLeafCount());
		LOG.info("Graph hash: "+clr.getHashGraph().getGraphHash());

		// the canonical labeling writes blank node using hashes w/o prefix
		// this code adds the prefix and maps them to URIs or blank nodes
		// as specified in the options
		LOG.info("Writing output ...");
		int written = 0;
		TreeSet<Node[]> canonicalGraph = clr.getGraph();
		GraphLabelIterator gli = new GraphLabelIterator(canonicalGraph.iterator(), prefix, writeBnode);
		while(gli.hasNext()){
			out.processStatement(gli.next());
			written ++;
		}
		LOG.info("... written "+written+" statements.");

		return clr;
	}

}
