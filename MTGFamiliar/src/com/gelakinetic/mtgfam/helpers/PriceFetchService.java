package com.gelakinetic.mtgfam.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Application;

import com.google.common.io.ByteProcessor;
import com.google.common.io.Files;
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
                        	return Files.readBytes(file, new ByteProcessor<PriceInfo>(){

								private PriceInfo pi;

								@Override
								public PriceInfo getResult() {
									return pi;
								}

								@Override
								public boolean processBytes(byte[] arg0, int arg1, int arg2) throws IOException {									
									byte toProcess[] = new byte[arg2-arg1];
									System.arraycopy(arg0, arg1, toProcess, 0, arg2-arg1);
									pi = new PriceInfo();
									pi.fromBytes(toProcess);
									return true;
									
								}});
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
                                    Files.write( data.toBytes(), getCacheFile( cacheKey ) );
                                } catch ( IOException e ) {
                                	// eat it
                                }
                            };
                        }.start();
                    } else {
                        Files.write( data.toBytes(), getCacheFile( cacheKey ));
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
}