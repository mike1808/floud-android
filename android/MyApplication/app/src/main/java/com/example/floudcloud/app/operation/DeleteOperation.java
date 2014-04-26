package com.example.floudcloud.app.operation;

import com.example.floudcloud.app.model.FileMove;
import com.example.floudcloud.app.network.FloudFile;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.network.exception.BadRequestError;
import com.example.floudcloud.app.network.exception.InternalServerError;
import com.example.floudcloud.app.network.exception.NotFoundError;
import com.example.floudcloud.app.utility.ProgressListener;

import retrofit.RetrofitError;

/**
 * Created by USER on 4/26/2014.
 */
public class DeleteOperation extends RemoteOperation {
    private FloudFile floudService;
    public DeleteOperation(String apiKey, String path) {
        super(null, null, path);
        floudService = new FloudService(apiKey).getFileService();
    }

    @Override
    public int execute(ProgressListener progressListener) {
        try {
            floudService.deleteFile(getPath());
        } catch (NotFoundError cause) {
            return 404;
        } catch (BadRequestError cause) {
            return 400;
        } catch (InternalServerError cause) {
            return 500;
        } catch (RetrofitError cause) {
            return 500;
        }

        return 200;
    }
}
