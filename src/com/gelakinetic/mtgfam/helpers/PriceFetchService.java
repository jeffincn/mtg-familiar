package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Application;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheLoadingException;
import com.octo.android.robospice.persistence.exception.CacheSavingException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;

public class PriceFetchService extends SpiceService {

    @Override
    public CacheManager createCacheManager( Application application ) {
        CacheManager cacheManager = new CacheManager();

        InFileObjectPersister<PriceInfo> priceInfoPersister = new InFileObjectPersister<PriceInfo>(application, PriceInfo.class) {
			
            @Override
            public PriceInfo loadDataFromCache( Object cacheKey, long maxTimeInCacheBeforeExpiry ) throws CacheLoadingException {
                File file = getCacheFile( cacheKey );
                if ( file.exists() ) {
                    long timeInCache = System.currentTimeMillis() - file.lastModified();
                    if ( maxTimeInCacheBeforeExpiry == 0 || timeInCache <= maxTimeInCacheBeforeExpiry ) {
                        try {
                        	return new PriceInfo(fileToBytes(file));
                        } catch ( FileNotFoundException e ) {
                            return null;
                        } catch ( Exception e ) {
                            throw new CacheLoadingException( e );
                        }
                    }
                }
                return null;
            }

            @Override
            public PriceInfo saveDataToCacheAndReturnData( final PriceInfo data, final Object cacheKey ) throws CacheSavingException {
                try {
                    if ( isAsyncSaveEnabled() ) {

                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getCacheFile( cacheKey )));
                                	bos.write(data.toBytes());
                                	bos.flush();
                                	bos.close();
                                } catch ( IOException e ) {
                                	return;
                                }
                            };
                        }.start();
                    } else {
                    	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getCacheFile( cacheKey )));
                    	bos.write(data.toBytes());
                    	bos.flush();
                    	bos.close();
                    }
                } catch ( Exception e ) {
                    throw new CacheSavingException( e );
                }
                return data;
            }
		};
        
        priceInfoPersister.setAsyncSaveEnabled(true);
        cacheManager.addPersister(priceInfoPersister);
        return cacheManager;
    }
    
	public byte[] fileToBytes(File file) throws IOException {

		byte[] buffer = new byte[(int) file.length()];
		InputStream ios = null;
		try {
			ios = new FileInputStream(file);
			if (ios.read(buffer) == -1) {
				throw new IOException(
						"EOF reached while trying to read the whole file");
			}
		} finally {
			try {
				if (ios != null)
					ios.close();
			} catch (IOException e) {
			}
		}

		return buffer;
	}
}