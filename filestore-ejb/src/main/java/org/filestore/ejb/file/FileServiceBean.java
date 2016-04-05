package org.filestore.ejb.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.filestore.ejb.store.BinaryStoreService;
import org.filestore.ejb.store.BinaryStoreServiceException;
import org.filestore.ejb.store.BinaryStreamNotFoundException;

@Stateless(name = "fileservice")
public class FileServiceBean implements FileService {
	
	private static final Logger LOGGER = Logger.getLogger(FileServiceBean.class.getName());
	
	@PersistenceContext(unitName="filestore-pu")
	protected EntityManager em;
	@Resource
	protected SessionContext ctx;
	@EJB
	protected BinaryStoreService store;
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String postFile(String owner, List<String> receivers, String message, String name, byte[] data) throws FileServiceException {
		LOGGER.log(Level.INFO, "Post File called (byte[])");
		return this.internalPostFile(owner, receivers, message, name, new ByteArrayInputStream(data));
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private String internalPostFile(String owner, List<String> receivers, String message, String name, InputStream stream) throws FileServiceException {
		try {
			String streamid = store.put(stream);
			String id = UUID.randomUUID().toString().replaceAll("-", "");
			FileItem file = new FileItem();
			file.setId(id);
			file.setOwner(owner);
			file.setReceivers(receivers);
			file.setMessage(message);
			file.setName(name);
			file.setStream(streamid);
			em.persist(file);
			
			//TODO send an email to owner and receivers
			return id;
		} catch ( BinaryStoreServiceException e ) {
			LOGGER.log(Level.SEVERE, "An error occured during storing binary content", e);
			ctx.setRollbackOnly();
			throw new FileServiceException("An error occured during storing binary content", e);
		} catch ( Exception e ) {
			LOGGER.log(Level.SEVERE, "unexpected error during posting file", e);
			throw new FileServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public FileItem getFile(String id) throws FileServiceException {
		LOGGER.log(Level.INFO, "Get File called");
		try {
			FileItem item = em.find(FileItem.class, id);
			if ( item == null ) {
				throw new FileServiceException("Unable to get file with id '" + id + "' : file does not exists");
			}
			return item;
		} catch ( Exception e ) {
			LOGGER.log(Level.SEVERE, "An error occured during getting file", e);
			throw new FileServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] getFileContent(String id) throws FileServiceException {
		LOGGER.log(Level.INFO, "Get File Content called");
		InputStream is = this.internalGetFileContent(id);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int len = 0;
			while ( (len=is.read(buffer)) != -1) {
			    baos.write(buffer, 0, len);
			}
		} catch (IOException e) {
			throw new FileServiceException("unable to copy stream", e);
		} finally {
			try {
				baos.flush();
				baos.close();
				is.close();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "error during closing streams", e);
			}
		}
		return baos.toByteArray();
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private InputStream internalGetFileContent(String id) throws FileServiceException {
		try {
			FileItem item = em.find(FileItem.class, id);
			if ( item == null ) {
				throw new FileServiceException("Unable to get file with id '" + id + "' : file does not exists");
			}
			InputStream is = store.get(item.getStream()); 
			return is;
		} catch ( BinaryStreamNotFoundException e ) {
			LOGGER.log(Level.SEVERE, "No binary content found for this file item !!", e);
			throw new FileServiceException("No binary content found for this file item !!", e);
		} catch ( BinaryStoreServiceException e ) {
			LOGGER.log(Level.SEVERE, "An error occured during reading binary content", e);
			throw new FileServiceException("An error occured during reading binary content", e);
		} catch ( Exception e ) {
			LOGGER.log(Level.SEVERE, "unexpected error during getting file", e);
			throw new FileServiceException(e);
		}
	}

}
