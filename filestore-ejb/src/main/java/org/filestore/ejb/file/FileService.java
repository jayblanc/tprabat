package org.filestore.ejb.file;

import java.util.List;

public interface FileService {
	
	public String postFile(String owner, List<String> receivers, String message, String name, byte[] data) throws FileServiceException;
	
	public FileItem getFile(String id) throws FileServiceException;
	
	public byte[] getFileContent(String id) throws FileServiceException;
	
}
