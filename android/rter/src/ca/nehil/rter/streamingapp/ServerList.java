package ca.nehil.rter.streamingapp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import ca.nehil.rter.streamingapp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ServerList extends Activity {
	ListView listView;
	SharedPreferences storedValues;
	SharedPreferences.Editor storedValuesEditor;
	ArrayList<String> server_list;
	Set<String> tempSet;
	String current_server;
	final String VALUES_SHAREDPREF_FILE = "CommonValues";
	Button addServerButton;

	@Override
	protected void onCreate(Bundle savedInstanceState){

		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_listview);
		
		// Fetch and store the active server and the stored server list.
		storedValues = getSharedPreferences(VALUES_SHAREDPREF_FILE, MODE_PRIVATE);
		storedValuesEditor = storedValues.edit();
		tempSet = new HashSet<String>();
		tempSet = storedValues.getStringSet("server_list", null);
		server_list = new ArrayList<String>();
		server_list.addAll(tempSet);
		current_server = storedValues.getString("server_url", "not-set");

		// Set up he listview with server data.
		listView = (ListView)findViewById(R.id.server_listview);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, android.R.id.text1, server_list);
		listView.setAdapter(adapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		restoreCheckedState(current_server, server_list, listView); //Restore the checked state of the server in use.

		listView.setOnItemClickListener(new OnItemClickListener() {
			// Activate the selected server.
			@Override
			public void onItemClick(AdapterView<?> parentView, View view, int position, long rowId) {
				current_server = server_list.get(position);
				storedValuesEditor.putString("server_url", current_server);
				storedValuesEditor.commit();
			}
		});

		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			// Delete the long clicked server.
			@Override
			public boolean onItemLongClick(AdapterView<?> parentView, View view, final int position, long rowId) {

				if(current_server.equals(server_list.get(position))){
					//Cannot delete the active server. Show error popup.
					Toast.makeText(ServerList.this, "Cannot delete an activated server.", Toast.LENGTH_SHORT).show();
				}else{
					// Ask for confirmation
					AlertDialog.Builder deleteServerAlert = new AlertDialog.Builder(ServerList.this);
					deleteServerAlert.setTitle(getString(R.string.delete_server_dialog_title));
					deleteServerAlert.setMessage(getString(R.string.delete_server_dialog_message) + "\n\n" + server_list.get(position));
					
					deleteServerAlert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Delete the server and update server_list and sharedprefs.
							server_list.remove(position);
							updateServerList();
						}
					});
					
					deleteServerAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Canceled.
						}
					});
					
					deleteServerAlert.show();
				}
				return true;
			}
		});

		addServerButton = (Button)findViewById(R.id.add_server_button);
		addServerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Dialogue box for new server
				AlertDialog.Builder addServerAlert = new AlertDialog.Builder(ServerList.this);

				addServerAlert.setTitle(getString(R.string.add_server_dialog_title));
				addServerAlert.setMessage(getString(R.string.add_server_dialog_message));
				
				// Set an EditText view to get user input 
				final EditText input = new EditText(ServerList.this);
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
				input.setText("http://");
				addServerAlert.setView(input);

				addServerAlert.setPositiveButton("Add Server", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						server_list.add(value);
						//update server list
						updateServerList();
					}
				});

				addServerAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				addServerAlert.show();
			}
		});
	}

	/*
	 * Restores the checked state of the activated server.
	 */
	private void restoreCheckedState(String current_server, ArrayList<String> server_list, ListView listView){
		int i;
		int position = -1;
		for(i = 0; i < server_list.size(); i++){
			if(current_server.equals(server_list.get(i))){
				position = i;
			}
		}
		assert position != -1; // A server must be set.
		listView.setItemChecked(position, true);
	}
	
	/*
	 * Called after any changes to the arrayList of servers is made, to update the sharedprefs
	 */
	private void updateServerList(){
		tempSet.clear();
		tempSet.addAll(server_list);
		storedValuesEditor.remove("server_list");
		storedValuesEditor.commit();
		storedValuesEditor.putStringSet("server_list", tempSet);
		storedValuesEditor.commit();
		((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
		restoreCheckedState(current_server, server_list, listView);
	}
}
