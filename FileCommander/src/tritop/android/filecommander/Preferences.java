package tritop.android.filecommander;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
	}
}
