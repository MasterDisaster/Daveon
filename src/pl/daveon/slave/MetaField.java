package pl.daveon.slave;

import java.io.Reader;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

public class MetaField  extends Field {

/* Indexed, tokenized, stored. */
public static final FieldType TYPE_STORED = new FieldType();

	static {
	    TYPE_STORED.setIndexed(true);
	    TYPE_STORED.setTokenized(false);
	    TYPE_STORED.setStored(true);
	    TYPE_STORED.setOmitNorms(true);
	    TYPE_STORED.setStoreTermVectors(false);
	    
	    TYPE_STORED.freeze();
	}
	
	// TODO: add sugar for term vectors...?
	
	/** Creates a new TextField with Reader value. */
	public MetaField(String name, Reader reader) {
	    super(name, reader, TYPE_STORED);
	}
	
	/** Creates a new TextField with String value. */
	public MetaField(String name, String value) {
	    super(name, value, TYPE_STORED);
	}
	
}