package com.dlab.rna.upload.api;

import com.dlab.common.model.Result;
import com.dlab.rna.upload.api.dto.InitRequest;
import com.dlab.rna.upload.api.dto.InitResponse;
import com.dlab.rna.upload.api.dto.StatusResponse;
import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.model.UploadTask;
import com.dlab.rna.upload.service.UploadService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private final UploadService service;
    private final RabbitTemplate rabbitTemplate;

    public UploadController(UploadService service, RabbitTemplate rabbitTemplate) {
        this.service = service;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/init")
    public Result<InitResponse> init(@Validated @RequestBody InitRequest req) {
        UploadTask task = service.initOrResume(req.getFilename(), req.getSize(), req.getChunkSize(), req.getSha256());
        InitResponse resp = new InitResponse(task.getFileId(), task.getStoragePath());
        resp.setChunkSize(task.getChunkSize());
        resp.setTotalChunks(service.totalChunks(task.getFileId()));
        resp.setUploadedIndices(service.uploadedIndices(task.getFileId()));
        resp.setResumed(resp.getUploadedIndices() != null && !resp.getUploadedIndices().isEmpty());
        return Result.success(resp);
    }

    @PutMapping("/{fileId}/chunk/{index}")
    public Result<String> uploadChunk(@PathVariable String fileId, @PathVariable int index, @RequestParam("file") MultipartFile file) throws Exception {
        String object = service.uploadChunk(fileId, index, file);
        return Result.success(object);
    }

    @PostMapping("/{fileId}/merge")
    public Result<Void> merge(@PathVariable String fileId) throws Exception {
        if (!service.canMerge(fileId)) return Result.error(400, "分片未完成");
        service.markMerging(fileId);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.MERGE_QUEUE, fileId);
        return Result.success();
    }

    @GetMapping("/{fileId}/status")
    public Result<StatusResponse> status(@PathVariable String fileId) {
        StatusResponse resp = new StatusResponse();
        resp.setState(service.getState(fileId));
        int done = service.doneChunks(fileId);
        int total = service.totalChunks(fileId);
        resp.setDoneChunks(done);
        resp.setTotalChunks(total);
        resp.setPercent(total == 0 ? 0.0 : (done * 1.0 / total));
        resp.setErrorMessage(service.getErrorMessage(fileId));
        return Result.success(resp);
    }

    @PostMapping("/{fileId}/abort")
    public Result<Void> abort(@PathVariable String fileId) {
        service.abort(fileId);
        return Result.success();
    }
}