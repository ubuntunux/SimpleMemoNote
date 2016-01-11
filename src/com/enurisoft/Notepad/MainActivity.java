package com.enurisoft.Notepad;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.content.Context;
import android.content.Context.*;
import android.database.*;
import android.net.*;
import android.provider.MediaStore.*;
import android.util.Log;

import java.util.*;
import java.text.*;
import java.io.File;
import java.io.FileOutputStream;
import android.widget.TextView.*;
import android.text.*;
import java.io.*;
import android.widget.AdapterView.*;

public class MainActivity extends Activity
{
	private final Activity me = this;
	
	ArrayList<HashMap<String, String>> myList;
	HashMap<Long, HashMap> myHashList;
	SimpleAdapter mAdapter;
	
	ListView 	mList_Memolist;
	
	EditText 	mText_Subject;
	EditText 	mText_Memo;
	Button 		mBtn_Save;
	Button 		mBtn_New;
	Button 		mBtn_Del;
	
	String 		mFilepath;
	
	int m_nOldLineCount = 0;
	long m_nCurTime = -1L;
	long m_nDeleteFileTime = -1L;
	boolean m_bLoaded =  false;
	boolean m_bModified = false;
	boolean m_bPopupDelete = false;
	boolean m_bHide_delete = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		m_nCurTime = -1L;
		m_nDeleteFileTime = -1L;
		m_bModified = false;
		m_bLoaded = false;
		m_bPopupDelete = false;
		
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		// get external filepath
		if(isExternalStorageWritable() && isExternalStorageReadable()) {
			mFilepath = me.getExternalFilesDir(null).getAbsolutePath();
		}
		else {
			// get internal path
			mFilepath = me.getFilesDir().getAbsolutePath();
		}
		
		// get ui
		mText_Subject = (EditText) findViewById(R.id.edit_subject);
		mText_Memo = (EditText) findViewById(R.id.edit_memo);
		mList_Memolist = (ListView) findViewById(R.id.listview);
		mBtn_Save = (Button) findViewById(R.id.btn_save);
		mBtn_New = (Button) findViewById(R.id.btn_new);
		mBtn_Del = (Button) findViewById(R.id.btn_del);
		
		// ListView
		myList = new ArrayList<HashMap<String, String>>();
		myHashList = new HashMap<Long, HashMap>();
		HashMap<String, String> map;
			
		// Load memo list
		File f = new File(mFilepath);
		String[] filenames = f.list(null);
		ArrayList<String> filelist = new ArrayList<String>();
		for (int i = 0; i < filenames.length; ++i){
            filelist.add(filenames[i]);
		}
		Collections.sort(filelist);
		for (int i = filelist.size()-1; i >= 0; --i){
            String filename = filelist.get(i);
			// split filename to date and name
			String[] splitFile = filename.split("_");
			if(splitFile.length > 1) {
				Long nTime = -1L;
				try {
					nTime = Long.valueOf(splitFile[0]);
					if(!myHashList.containsKey(nTime)) {
						// set memo date
						map = new HashMap<String, String>();
						String szDate = timeToString(nTime);
						map.put("time", String.valueOf(nTime));
						map.put("date", szDate);
						// add to list
						myHashList.put(nTime, map);
						myList.add(map);
					} else {
						map = myHashList.get(nTime);
					}
				} catch(Exception e) {
					e.printStackTrace();
					continue;
				}
				// load memo or subject
				if(splitFile[1].indexOf("memo.txt")>-1) {
					map.put("memo", loadString(filename));
				}
				else if(splitFile[1].indexOf("subject.txt")>-1) {
					map.put("subject", loadString(filename));
				}
			}
		}

		// set adapter
		mAdapter = new SimpleAdapter(this, myList, R.layout.row,
									 new String[] {"date", "subject", "memo"}, new int[] {R.id.CELL_Date, R.id.CELL_Subject, R.id.CELL_Memo});
		mList_Memolist.setAdapter(mAdapter);
		setListener();
	}
	
	// set listener
	public void setListener() {
		// memo item on click
		mList_Memolist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView parentView, View childView, 
										int position, long id) 
				{
					// if opened delete popup window then return
					if(m_bPopupDelete) {
						return;
					}
					// onClick - set memo to editor
					HashMap<String, String> map = myList.get(position);
					long nTime = Long.valueOf(map.get("time"));
					String szDate = timeToString(nTime);
					// set cur file time
					m_nCurTime = nTime;
					m_bLoaded = true;
					mText_Subject.setText(map.get("subject"));
					mText_Memo.setText(map.get("memo"));
					refresh_memo_editor();
					m_bLoaded = false;
				}
			});
		// delete item on long click list
		mList_Memolist.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView parentView, View childView, 
											   int position, long id)
				{
					HashMap<String, String> map = myList.get(position);
					long nTime = Long.valueOf(map.get("time"));
					delete_dialog(nTime);
					return false;
				}
		});
		// on changed subject text view	
		mText_Subject.addTextChangedListener(new TextWatcher(){
				public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {}
				public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {}
				public void afterTextChanged(Editable s) {
					if(!m_bLoaded) {
						m_bModified = true;
					}
				}
			});
		// on changed memo text view
		mText_Memo.addTextChangedListener(new TextWatcher(){
				public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {}
				public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {}
				public void afterTextChanged(Editable s) {
					if(!m_bLoaded) {
						m_bModified = true;
						refresh_memo_editor();
					}
				}
			});
		// new button on click
		mBtn_New.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View parentView) 
				{
					// ask save and clear
					if(m_bModified) {
						save_dialog();
					} else {
						clear_memo();
					}
				}
			});
		// save button on click
		mBtn_Save.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View parentView) 
				{
					save_memo();
					clear_memo();
				}
			});
		// delete button on click
		mBtn_Del.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View parentView) 
				{
					if(myHashList.containsKey(m_nCurTime)) {
						// delete memo
						delete_dialog(m_nCurTime);
					}
				}
			});
	}
	
	// press back
	@Override
	public void onBackPressed()
	{
		if(mText_Subject.getText().toString().isEmpty() &&
			mText_Memo.getText().toString().isEmpty()) {
			 super.onBackPressed();
			 return;
		 } else if(m_bModified) {
			save_dialog();
		} else {
			clear_memo();
		}
	}
	
	// toast message
	public void makeToast(String message) {
		Toast saved = Toast.makeText(me, message, 1);
		saved.setGravity(Gravity.BOTTOM, 0, 0);
		saved.show();
	}
	
	// clear memo
	public void clear_memo() {
		mText_Subject.setText("");
		mText_Memo.setText("");
		refresh_memo_editor();
		m_bModified = false;
		m_nCurTime = -1L;
	}
	
	// refresh memo editor
	public void refresh_memo_editor() {
		if(mText_Memo.getLineCount() == m_nOldLineCount) {
			return;
		}
		m_nOldLineCount = mText_Memo.getLineCount();
		mText_Memo.setHeight(mText_Memo.getLineCount() * mText_Memo.getLineHeight()
			+ mText_Memo.getPaddingBottom() + mText_Memo.getPaddingEnd());
	}
	
	// save memo
	public void save_memo() {
		if(!m_bModified) {
			if(m_nCurTime != -1L) {
				String subject = mText_Subject.getText().toString();
				// set toast
				if(subject.isEmpty()) {
					makeToast(getString(R.string.saved));
				} else {
					makeToast(subject + "\n" + getString(R.string.saved));
				}
			}
			return;
		}
		
		m_bModified = false;
		String subject = mText_Subject.getText().toString();
		String memo = mText_Memo.getText().toString();
		long nTime = -1L;
		String szDate, szTime;
		
		// add new memo and save
		if(!myHashList.containsKey(m_nCurTime)) {
			nTime = System.currentTimeMillis();
			szDate = timeToString(nTime);
			szTime = String.valueOf(nTime);
			m_nCurTime = nTime;
			
			// add memo
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("time", szTime);
			map.put("date", szDate);
			map.put("subject", subject);
			map.put("memo", memo);

			// add to list
			myList.add(0, map);
			myHashList.put(nTime, map);
			mAdapter.notifyDataSetChanged();
		}
		// exsiting memo
		else {
			nTime = m_nCurTime;
			szDate = timeToString(nTime);
			szTime = String.valueOf(nTime);
			HashMap<String, String> map = myHashList.get(nTime);
			map.put("time", szTime);
			map.put("date", szDate);
			map.put("subject", subject);
			map.put("memo", memo);
			mAdapter.notifyDataSetChanged();
		}
		
		// save memo file
		String[] prefix = new String[] {"_subject.txt", "_memo.txt"};
		String[] content = new String[] {subject, memo};
		for(int i=0;i<2;++i) {
			String filename = szTime + prefix[i];
			saveString(filename, content[i].toString());
		}

		// set toast
		if(subject.isEmpty()) {
			makeToast(getString(R.string.saved));
		} else {
			makeToast(subject + "\n" + getString(R.string.saved));
		}
	}
	
	// save dialog
	public void save_dialog() {
		// build message string, get subject
		String message = getString(R.string.want_save);
		String subject;
		if(myHashList.containsKey(m_nCurTime)) {
			HashMap<String, String> map = myHashList.get(m_nCurTime);
			subject = map.get("subject");
		} else {
			subject = mText_Subject.getText().toString();
		}
		
		if(!subject.isEmpty()) {
			message = subject + "\n" + message;
		}
		
		// create dialog
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(me);
		alt_bld.setMessage(message).setCancelable(
			false).setPositiveButton(getString(R.string.yes),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Action for 'Yes' Button
					save_memo();
					clear_memo();
				}
			}).setNegativeButton(getString(R.string.no),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Action for 'NO' Button
					dialog.cancel();
					clear_memo();
				}
			});
		AlertDialog alert = alt_bld.create();
		// Title for AlertDialog
		alert.setTitle(getString(R.string.save));
		// Icon for AlertDialog
		//alert.setIcon(R.drawable.ic_launcher);
		alert.show();
	}
	
	// delete memo file
	public boolean delete_memo(long nTime) {
		if(myHashList.containsKey(nTime)) {
			String szTime = String.valueOf(nTime);
			String[] prefix = new String[] {"_subject.txt", "_memo.txt"};
			for(int i=0;i<2;++i) {
				String filename = szTime + prefix[i];
				File file = new File(mFilepath, filename);
				file.delete();
			}
			HashMap<String, String> map = myHashList.get(nTime);
			String subject = map.get("subject");
			myList.remove(map);
			myHashList.remove(nTime);
			mAdapter.notifyDataSetChanged();
			
			// if current memo deleted then clear memo
			if(m_nCurTime == nTime) {
				clear_memo();
			}
			
			// set toast
			String message = getString(R.string.deleted);
			if(!subject.isEmpty()) {
				message = subject + "\n" + message;
			}
			makeToast(message);
			return true;
		}
		return false;
	}
	
	// delete dialog
	public void delete_dialog(long nTime) {
		m_bPopupDelete = true;
		m_nDeleteFileTime = nTime;
		// build message string, get subject
		String message = getString(R.string.want_delete);
		if(myHashList.containsKey(nTime)) {
			HashMap<String, String> map = myHashList.get(nTime);
			String subject = map.get("subject");
			if(!subject.isEmpty()) {
				message = subject + "\n" + message;
			}
		}
		
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(me);
		alt_bld.setMessage(message).setCancelable(
			false).setPositiveButton(getString(R.string.yes),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Action for 'Yes' Button
					delete_memo(m_nDeleteFileTime);
					m_bPopupDelete = false;
				}
			}).setNegativeButton(getString(R.string.no),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Action for 'NO' Button
					dialog.cancel();
					m_bPopupDelete = false;
				}
			});
		AlertDialog alert = alt_bld.create();
		alert.setTitle(getString(R.string.delete));
		alert.show();
	}
		
	// time convert to string
	public String timeToString(long nTime) {
		Date date = new Date(nTime);
		SimpleDateFormat CurDateFormat = new SimpleDateFormat("yyyy/MM/dd  aa HH:mm:ss");
		return CurDateFormat.format(date);
	}
	
	// load String from file
	public String loadString(String filename) {
		try{
			File file = new File(mFilepath, filename);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(file), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				return str;
			}
			in.close();
		} catch(Exception e){
			e.printStackTrace();
			Log.d("io error", "IO ERROR");
		}
		return "";
	}
	
	// save String
	public void saveString(String filename, String str) {
		try{
			File file = new File(mFilepath, filename);
			Writer out = new BufferedWriter(
				new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"));		
			out.write(str);
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean isExternalStorageWritable(){
		String state = Environment.getExternalStorageState();
		if( Environment.MEDIA_MOUNTED.equals(state)){
			return true;
		}
		return false;
	}

	public boolean isExternalStorageReadable(){
		String state = Environment.getExternalStorageState();
		if( Environment.MEDIA_MOUNTED.equals(state) ||
		   Environment.MEDIA_MOUNTED_READ_ONLY.equals( state)){
			return true;
		}
		return false;
	}
}
