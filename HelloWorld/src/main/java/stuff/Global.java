package stuff;

public class Global
{
	private static Global self;
	//change this to true when deploying to the server, false when testing locally:
	public final static boolean PROD = false;

	private Global()
	{
	}

	public static Global getInstance()
	{
		if(self == null)
		{
			self = new Global();
		}
		return self;
	}
}