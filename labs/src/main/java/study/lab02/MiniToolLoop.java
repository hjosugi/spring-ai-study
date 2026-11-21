package study.lab02;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The loop at the heart of Spring AI's {@code ToolCallingAdvisor}, without the parts
 * that make the real one production-ready (tool call limits, confirmation, return
 * direct, usage accumulation, streaming, {@code ToolCallingManager}).
 *
 * <p>
 * Tools are plain functions from the JSON arguments string to a result string.
 */
public final class MiniToolLoop implements MiniChain.MiniAdvisor {

	private final Map<String, Function<String, String>> tools;

	public MiniToolLoop(Map<String, Function<String, String>> tools) {
		this.tools = Map.copyOf(tools);
	}

	@Override
	public ChatResponse advise(Prompt prompt, MiniChain chain) {
		List<Message> messages = new ArrayList<>(prompt.getInstructions());

		while (true) {
			// The chain was consumed by the previous round, so take a fresh copy of
			// everything that comes after this advisor.
			ChatResponse response = chain.copyAfter(this).next(new Prompt(messages, prompt.getOptions()));
			if (!response.hasToolCalls()) {
				return response;
			}

			// The model asked for tools: run them and send the conversation back with
			// the request and the results appended.
			AssistantMessage assistantMessage = response.getResult().getOutput();
			List<ToolResponse> toolResponses = assistantMessage.getToolCalls()
				.stream()
				.map(call -> new ToolResponse(call.id(), call.name(), runTool(call.name(), call.arguments())))
				.toList();

			messages.add(assistantMessage);
			messages.add(ToolResponseMessage.builder().responses(toolResponses).build());
		}
	}

	private String runTool(String name, String jsonArguments) {
		Function<String, String> tool = this.tools.get(name);
		if (tool == null) {
			throw new IllegalStateException("The model asked for an unknown tool: " + name);
		}
		return tool.apply(jsonArguments);
	}

}
