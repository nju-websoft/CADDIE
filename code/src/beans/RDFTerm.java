package beans;

public class RDFTerm {
    String iri;
    String label;
    int type; // 0: IRI, 1: blank node, 2: literal

    public RDFTerm(String iri, String label, int type) {
        this.iri = iri;
        this.label = (label == null ? "" : label);
        this.type = type;
    }

    public String getIri() {
        return iri;
    }

    public String getLabel() {
        return label;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (! (obj instanceof RDFTerm)) {
            return false;
        }
        RDFTerm term = (RDFTerm) obj;
        return term.type == this.type && term.iri.equals(this.iri);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type;
        result = prime * result + ((iri == null) ? 0 : iri.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return type + ":" + label;
    }

}
