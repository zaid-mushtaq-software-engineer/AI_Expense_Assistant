package com.expenseassistant.assistant.tools;

import com.expenseassistant.assistant.service.ChatTurnContext;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;

final class AssistantToolSupportTestHelper {

    private AssistantToolSupportTestHelper() {}

    static AssistantToolSupport toolSupport(ChatTurnContext chatTurnContext) {
        ObjectProvider<ChatTurnContext> provider =
                new ObjectProvider<>() {
                    @Override
                    public ChatTurnContext getObject() {
                        return chatTurnContext;
                    }

                    @Override
                    public ChatTurnContext getObject(Object... args) {
                        return chatTurnContext;
                    }

                    @Override
                    public ChatTurnContext getIfAvailable() {
                        return chatTurnContext;
                    }

                    @Override
                    public ChatTurnContext getIfUnique() {
                        return chatTurnContext;
                    }

                    @Override
                    public void ifAvailable(Consumer<ChatTurnContext> action) {
                        action.accept(chatTurnContext);
                    }

                    @Override
                    public void ifUnique(Consumer<ChatTurnContext> action) {
                        action.accept(chatTurnContext);
                    }
                };
        return new AssistantToolSupport(provider);
    }
}