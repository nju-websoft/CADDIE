package cl.uchile.dcc.blabel.lean.util;

import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;

import java.util.ArrayList;

public class Bindings {
	private final ArrayList<BNode> output;
	private final ArrayList<Node[]> bindings;
	
	public Bindings(ArrayList<BNode> output, ArrayList<Node[]> bindings){
		this.output = output;
		this.bindings = bindings;
	}

	public ArrayList<BNode> getOutput() {
		return output;
	}

	public ArrayList<Node[]> getBindings() {
		return bindings;
	}
}
