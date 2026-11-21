package study.lab01;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * A {@link ChatModel} that needs no provider and no API key: it replays a fixed script
 * of assistant replies and remembers every {@link Prompt} it was called with.
 *
 * <p>
 * Implementing the one abstract method, {@link #call(Prompt)}, is all a model has to do
 * to be usable from {@code ChatClient}.
 */
public final class ScriptedChatModel implements ChatModel {

	private final Deque<AssistantMessage> script = new ArrayDeque<>();

	private final List<Prompt> receivedPrompts = new ArrayList<>();

	public ScriptedChatModel(AssistantMessage... replies) {
		this.script.addAll(List.of(replies));
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		this.receivedPrompts.add(prompt);
		AssistantMessage reply = this.script.poll();
		if (reply == null) {
			throw new IllegalStateException("Script exhausted after " + this.receivedPrompts.size() + " calls");
		}
		return new ChatResponse(List.of(new Generation(reply)));
	}

	/**
	 * ChatClient starts from these options and adds the per-request tool callbacks to
	 * them. The tool calling advisor only runs its loop when the prompt options are
	 * {@link ToolCallingChatOptions}, so a model that wants tool calling has to return
	 * that type here. The default in {@link ChatModel} returns plain {@link ChatOptions}.
	 */
	@Override
	public ChatOptions getOptions() {
		return ToolCallingChatOptions.builder().build();
	}

	/** Every prompt this model was called with, in order. */
	public List<Prompt> receivedPrompts() {
		return List.copyOf(this.receivedPrompts);
	}

}
