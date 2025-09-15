package test.dev.demo.business.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.dev.demo.business.application.service.GrpcService;
import test.dev.streaming.proto.ChangeRequest;
import test.dev.streaming.proto.ChangeResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GrpcController {

    private final GrpcService grpcService;

    @GetMapping("/grpc")
    public ChangeResponse callGrpc(@RequestBody ChangeRequest request) {
        return grpcService.callGrpcService(request);
    }

}
