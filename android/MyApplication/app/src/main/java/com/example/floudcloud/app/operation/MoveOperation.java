package com.example.floudcloud.app.operation;

import com.example.floudcloud.app.model.FileMove;
import com.example.floudcloud.app.network.FloudFile;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.network.exception.BadRequestError;
import com.example.floudcloud.app.network.exception.InternalServerError;
import com.example.floudcloud.app.network.exception.NotFoundError;
import com.example.floudcloud.app.utility.ProgressListener;

import retrofit.RetrofitError;


public class MoveOperation extends RemoteOperation {
    private String oldPath;
    private FloudFile floudService;
    public MoveOperation(String apiKey, String newPath, String oldPath) {
        super(apiKey, null, newPath);
        this.oldPath = oldPath;
        floudService = new FloudService(apiKey).getFileService();
    }

    @Override
    public int execute(ProgressListener listener) {
        try {
            floudService.moveFile(new FileMove(getPath(), oldPath));
        } catch (BadRequestError cause) {
            return 400;
        } catch (InternalServerError cause) {
            return 500;
        } catch (NotFoundError cause) {
            return 404;
        } catch (Exception e) {
            return 500;
        }

        return 200;
    }
}
