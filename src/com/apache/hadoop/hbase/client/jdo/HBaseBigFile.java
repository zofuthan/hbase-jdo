package com.apache.hadoop.hbase.client.jdo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * HBaseBigFile
 * HBase cannot upload big file, so you should use hadoop directly.
 * 
 * for example.
 *  <code>
 *  File file = new File("doc/movie.avi");
	FileInputStream fis = new FileInputStream(file);
	Path rootPath = new Path("/files/");
	String filename = "movie.avi";
	boolean isUploaded = ht.uploadFile(rootPath,filename,fis,true);
	</code>		
 * @author ncanis
 *
 */
public class HBaseBigFile implements IHBaseLog{
	private Configuration conf = null;
	public HBaseBigFile(){
		this(new Configuration());
	}
	
	public HBaseBigFile(Configuration conf){
		this.conf = conf;
	}
	
	private FileSystem getDFS() throws IOException {
		return FileSystem.get(conf);
	}
	
	public void close(OutputStream os){
		if(os==null) return;
		try { os.close(); } catch (IOException e) {}
	}
	public void close(InputStream is){
		if(is==null) return;
		try { is.close(); } catch (IOException e) {}
	}
	
	/**
	 * upload file.
	 * @param directory hadoop directory will be saved uploaded file. 
	 * @param filename file name.
	 * @param is file stream
	 * @param overwrite if true if the file is exist, it'll be overwrited.
	 * @return
	 */
	public boolean uploadFile(Path directory,String filename, InputStream is, boolean overwrite){
		boolean isSuccess=false;
		FSDataOutputStream fos=null;		
		try {
			Path p  = new Path(directory, new Path(filename));
			FileSystem fs = getDFS();
			if(fs.getFileStatus(directory).isDir()==false) {
				throw new IOException(directory+" isn't directory.");				
			}else if(fs.exists(p)){
				if(overwrite) {
					delete(p,true);
				}else{
					throw new IOException(p+" already exist.");
				}
			}
			
			fos = fs.create(p);
			BufferedInputStream bis = new BufferedInputStream(is);
			IOUtils.copyBytes(bis,fos,8192,true);
//			copyBytes(bis,fos,8192,true);
						
			isSuccess= true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			close(fos);
		}
		return isSuccess;
	}
	
	/**
	 * copy bytes.
	 * you should use IOUtils.copyBytes(bis,fos,8192,true)
	 * @param is
	 * @param os
	 * @param bufferSize
	 * @param isLog
	 * @throws IOException
	 */
	private void copyBytes(InputStream is, OutputStream os, int bufferSize, boolean isLog) throws IOException{
		int length=0;
		long current = 0;		
		long total = is.available();
		long progress = 0;
		byte[] buf = new byte[bufferSize];
		while((length=is.read(buf))!=(-1)){
			os.write(buf,0, length);
			current+=length;
			long now = current*100/total;
			if(progress!=now) {			
				log.debug("progress={}%",now);
				progress = now;
			}
		}
	}
	
	/**
	 * delete file/directory
	 * @param p
	 * @param recursive
	 * @throws IOException
	 */
	public void delete(Path p, boolean recursive) throws IOException{
		FileSystem fs = getDFS();
		fs.delete(p, recursive);
	}
	
	/**
	 * after use, must close inputstream.
	 * @param p
	 * @param bufferSize
	 * @return
	 * @throws IOException
	 */
	public InputStream path2Stream(Path p, int bufferSize) throws IOException{
		FileSystem fs = getDFS();
		FSDataInputStream fis = fs.open(p, bufferSize);
		return fis;
	}
	
	/**
	 * copy file from hadoop to local.
	 * @param path hadoop path
	 * @param f local file location.
	 * @return
	 */
	public boolean copyFile2Local(Path path, File f){
		boolean isSuccess=false;
		try {
			InputStream is = path2Stream(path,8192);
			write2File(is,f);
			isSuccess= true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isSuccess;
		
	}
	/**
	 * load file stream in hbase, write to file
	 * @param is
	 * @param f
	 * @return
	 */
	public boolean write2File(InputStream is, File f){
		boolean isSuccess=false;
		FileOutputStream fos=null;
		try {
			
			BufferedInputStream bis = new BufferedInputStream(is);
			fos = new FileOutputStream(f);
//			IOUtils.copyBytes(is,fos,8192,true);	
			copyBytes(bis,fos,8192,true);
			isSuccess = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			close(is);
			close(fos);
		}
		return isSuccess;
	}
}