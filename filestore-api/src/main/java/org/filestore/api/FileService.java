package org.filestore.api;

import java.util.List;

import javax.activation.DataHandler;
import javax.ejb.Remote;

@Remote
public interface FileService {
	
	public String postFile(String owner, List<String> receivers, String message, String name, byte[] data) throws FileServiceException;
	
	public String postFile(String owner, List<String> receivers, String message, String name, DataHandler data) throws FileServiceException;
	
	public FileItem getFile(String id) throws FileServiceException;
	
	public byte[] getWholeFileContent(String id) throws FileServiceException;
	
	public DataHandler getFileData(String id) throws FileServiceException;

}
