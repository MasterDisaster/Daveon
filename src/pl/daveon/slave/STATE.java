package pl.daveon.slave;


/**
 * @author bkaczmarek
 *
 *
 *
 */
public enum STATE{
	STORED(1), NOTSTORED(2), UNDEF(3);

	int _type = 2;
	STATE(final int type){
		this._type=type;
	}
}
