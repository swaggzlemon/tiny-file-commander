/* 
 * Copyright (C) 2011 Christian Schneider
 * 
 * This file is part of Tiny File Commander.
 * 
 * Tiny File Commander is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tiny File Commander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tiny File Commander.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package tritop.android.filecommander;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class StorageViewAdapter extends BaseAdapter {

	private String mCurrentPath;
	private Context mContext;
	private File mRootDir;
	private String[] mFiles={};
	private List<String> lmfiles = new ArrayList<String>();
	private List<String> lmdirs = new ArrayList<String>();
	
	
	//*************************************************************************
	// Constructor with path
	//*************************************************************************
	
	StorageViewAdapter(Context ctx,String path){
		mContext = ctx;
		mCurrentPath ="";
		this.relocateToRoot(path);
	}
	
	
	//*************************************************************************
	// Constructor without path, needs a relocateToRoot followup call
	//*************************************************************************
	
	StorageViewAdapter(Context ctx){
		mContext = ctx;
		mCurrentPath ="";
	}
	
	
	//*************************************************************************
	// set new look in directory
	//*************************************************************************
	
	public boolean relocateToRoot(String newPath){
		Log.e("ADAPTER","PATH: "+newPath);
		if(mCurrentPath.equals(newPath)){
			return true;
		}
		else{
			File tmp = new File(newPath);
			if(tmp.isDirectory()){
				mCurrentPath=newPath;
				mRootDir=tmp;
				this.reloadContent(mRootDir);
				return true;
			}
		}
		return false;
	}
	
	
	//*************************************************************************
	// returns current look in path
	//*************************************************************************
	
	public String currentPath(){
		return mRootDir.getAbsolutePath();
	}
	
	
	//*************************************************************************
	// called from relocateToRoot build new file list -> notify adapter view
	//*************************************************************************
	
	private void reloadContent(File dir){
		mFiles=dir.list();
		this.sortFileList();
		this.notifyDataSetChanged();
	}
	
	
	public void reloadCurrentDir(){
		if(mRootDir != null && mRootDir.isDirectory()){
			this.reloadContent(mRootDir);
		}
	}
	
	//*************************************************************************
	// Sorts file list, dirs to front 
	//*************************************************************************
	
	private void sortFileList(){
		if(mFiles!=null && mFiles.length>0){
			lmfiles.clear();
			lmdirs.clear();
			List<String> allfiles = new ArrayList<String>();
			for(String filename : mFiles){
				File chk = new File(mRootDir.getAbsolutePath(),filename);
				if(chk.isDirectory()){
					lmdirs.add(filename);
				}
				else {
					lmfiles.add(filename);
				}
			}
			Collections.sort(lmdirs);
			Collections.sort(lmfiles);
			allfiles.addAll(lmdirs);
			allfiles.addAll(lmfiles);
			mFiles =  allfiles.toArray(new String[allfiles.size()]);
		}
	}
	
	@Override
	public int getCount() {
		if(mFiles!=null){
			return mFiles.length;
		}
		else return 0;
	}

	
	//*************************************************************************
	// is Filename at adapter[position] a directory?
	//*************************************************************************
	
	public boolean isDir(int position){
		if(lmdirs.contains(mFiles[position])){
			return true;
		}
		return false;
	}
	
	
	//*************************************************************************
	// is Filename at adapter[position] a file?
	//*************************************************************************
	
	public boolean isFile(int position){
		if(lmfiles.contains(mFiles[position])){
			return true;
		}
		return false;
	}
	
	
	//*************************************************************************
	// return path at adapter[position]
	//*************************************************************************
	
	public String getPath(int position){
		return mRootDir.getAbsolutePath()+"/"+mFiles[position];
	}
	
	
	//*************************************************************************
	// jump into directory at adapter[position]
	//*************************************************************************
	
	public boolean relocateToPosition(int position){
		if(lmdirs.contains(mFiles[position])){
			this.relocateToRoot(mRootDir.getAbsolutePath()+"/"+mFiles[position]);
			return true;
		}
		return false;
	}
	
	
	//*************************************************************************
	// move one directory back
	//*************************************************************************
	
	public boolean moveUp(){
		if(mRootDir != null && mRootDir.getParent()!=null){
			return this.relocateToRoot(mRootDir.getParent());
		}
		return false;
	}
	
	
	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inf = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inf.inflate(R.layout.fileview, parent, false);
		TextView tvFilename = (TextView) convertView.findViewById(R.id.textViewFilename);
		ImageView imFile = (ImageView) convertView.findViewById(R.id.imageViewFileIcon);
		if(lmdirs.contains(mFiles[position])){
			imFile.setImageResource(R.drawable.blue_glossy_dirbaseb100);
		}
		else {
			imFile.setImageResource(R.drawable.blue_glossy_filebaseb100);
		}
		tvFilename.setText(mFiles[position]);
		return convertView;
	}

}
