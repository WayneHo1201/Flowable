package com.fwd.fsm.flowable.service.flow;

import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import com.fwd.fsm.flowable.domain.vo.FlowProcDefVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class FlowDefinitionService {
    private static final String BPMN_FILE_SUFFIX = ".bpmn20.xml";
    @Autowired
    private RepositoryService repositoryService;

    public FlowProcDefVO deployFlow(String name, String category, String tenantId, MultipartFile file) {
        InputStream in = null;
        FlowProcDefVO vo;
        try {
            in = file.getInputStream();
            ProcessDefinition processDefinition = importFile(name, category, tenantId, in);
            vo = convertProcDef(processDefinition);
        } catch (Exception e) {
            log.error("import file fail", e);
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("fail to close the inputStream", e);
            }
        }
        return vo;
    }

    private ProcessDefinition importFile(String name, String category, String tenantId, InputStream inputStream) {
        // deploy flow
        Deployment deploy = repositoryService.createDeployment()
                .addInputStream(name + BPMN_FILE_SUFFIX, inputStream)
                .name(name)
                .tenantId(tenantId)
                .category(category)
                .deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
        repositoryService.setProcessDefinitionCategory(definition.getId(), category);
        return definition;
    }

    public FlowProcDefVO getProcDef(String key, Integer version) {
        FlowProcDefVO vo;
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).list();
        if (version == null) {
            // query latest version
            vo = list.stream()
                    .map(this::convertProcDef)
                    .max(Comparator.comparingInt(FlowProcDefVO::getVersion))
                    .orElse(null);
        } else {
            // query specific version
            vo = list.stream()
                    .filter(processDefinition -> version == processDefinition.getVersion())
                    .map(this::convertProcDef)
                    .findFirst().orElse(null);
        }
        return vo;
    }

    public List<FlowProcDefVO> getProcDefList(String key) {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).list();
        return convertProcDefList(list);
    }


    private List<FlowProcDefVO> convertProcDefList(List<ProcessDefinition> procDefList) {
        return procDefList.stream().map(this::convertProcDef).toList();
    }

    /**
     * convert processDefinition to vo
     */
    private FlowProcDefVO convertProcDef(ProcessDefinition processDefinition) {
        return FlowProcDefVO.builder()
                .processDefinitionId(processDefinition.getId())
                .flowKey(processDefinition.getKey())
                .deploymentId(processDefinition.getDeploymentId())
                .name(processDefinition.getName())
                .category(processDefinition.getCategory())
                .version(processDefinition.getVersion())
                .description(processDefinition.getDescription())
                .build();
    }

    /**
     * read bpmn xml
     */
    @SneakyThrows
    public String readXml(String processInstanceId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstanceId).singleResult();
        InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName());
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }


    public void readImage(String processInstanceId,  HttpServletResponse response) {
        OutputStream os = null;
        BufferedImage image;
        try {
            image = ImageIO.read(readImage(processInstanceId));
            response.setContentType("image/png");
            os = response.getOutputStream();
            if (image != null) {
                ImageIO.write(image, "png", os);
            }
        } catch (Exception e) {
            log.error("get image stream fail", e);
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
               log.error("shutdown io stream fail", e);
            }
        }

    }


    /**
     * read image
     */
    public InputStream readImage(String processInstanceId) {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstanceId).singleResult();
        //generate image stream
        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        return diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                1.0,
                false);

    }

    public String deleteDeploymentById(String deployId) {
        repositoryService.deleteDeployment(deployId, true);
        return deployId;
    }

}
