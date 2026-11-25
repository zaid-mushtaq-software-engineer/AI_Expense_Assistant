package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.PendingActionResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ChatTurnContext {

    private PendingActionResponse pendingAction;
    private final List<String> toolsUsed = new ArrayList<>();
    private final List<String> toolErrors = new ArrayList<>();

    public Optional<PendingActionResponse> getPendingAction() {
        return Optional.ofNullable(pendingAction);
    }

    public void setPendingAction(PendingActionResponse pendingAction) {
        this.pendingAction = pendingAction;
    }

    public void recordTool(String toolName, boolean failed) {
        toolsUsed.add(toolName);
        if (failed) {
            toolErrors.add(toolName);
        }
    }

    public List<String> getToolsUsed() {
        return Collections.unmodifiableList(toolsUsed);
    }

    public List<String> getToolErrors() {
        return Collections.unmodifiableList(toolErrors);
    }
}