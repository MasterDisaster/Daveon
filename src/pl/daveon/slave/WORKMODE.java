package pl.daveon.slave;

/**
 * @author BKaczmarek
 * Okresla tryb pracy indexera
 */
public enum  WORKMODE{

	CROWLING_MODE(1), FULLINDEXING_MODE(2), OPTIMIZE(3);

	int _type = 2;
	WORKMODE(final int type){
		this._type=type;
	}
}