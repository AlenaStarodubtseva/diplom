package ru.bgpu.certificates.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateDocumentRequest {

    private List<Long> requestIds;

    private String actorLogin;

    private String actorRole;
}