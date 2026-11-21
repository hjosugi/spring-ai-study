package study.lab02;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import study.lab01.ScriptedChatModel;
import study.lab02.MiniChain.MiniAdvisor;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Lab 02: the hand-written chain behaves like the real one did in Lab 01.
 */
class MiniChainTest {

	private final List<String> log = new ArrayList<>();

	private MiniAdvisor recording(String name) {
		return (prompt, chain) -> {
			this.log.add(name + ":before");
			ChatResponse response = chain.next(prompt);
			this.log.add(name + ":after");
			return response;
		};
	}

	@Test
	void advisorsWrapTheModelCall() {
		ScriptedChatModel model = new ScriptedChatModel(new AssistantMessage("hello"));
		MiniChain chain = MiniChain.of(model, List.of(recording("outer"), recording("inner")));

		ChatResponse response = chain.next(new Prompt("hi"));

		assertThat(response.getResult().getOutput().getText()).isEqualTo("hello");
		assertThat(this.log).containsExactly("outer:before", "inner:before", "inner:after", "outer:after");
	}

	@Test
	void aChainCanOnlyBeRunOnce() {
		ScriptedChatModel model = new ScriptedChatModel(new AssistantMessage("one"), new AssistantMessage("two"));
		MiniChain chain = MiniChain.of(model, List.of());

		chain.next(new Prompt("hi"));

		// This is why a looping advisor has to copy the chain for every round.
		assertThatIllegalStateException().isThrownBy(() -> chain.next(new Prompt("again")));
	}

	@Test
	void toolLoopRunsTheRestOfTheChainOncePerRound() {
		ScriptedChatModel model = new ScriptedChatModel(
				AssistantMessage.builder()
					.content("")
					.toolCalls(List.of(new ToolCall("call-1", "function", "get_weather", "{\"city\":\"Tokyo\"}")))
					.build(),
				new AssistantMessage("It is sunny in Tokyo."));

		List<String> toolArguments = new ArrayList<>();
		MiniToolLoop toolLoop = new MiniToolLoop(Map.of("get_weather", arguments -> {
			toolArguments.add(arguments);
			return "sunny";
		}));

		MiniChain chain = MiniChain.of(model, List.of(recording("outsideLoop"), toolLoop, recording("insideLoop")));

		ChatResponse response = chain.next(new Prompt("Weather in Tokyo?"));

		assertThat(response.getResult().getOutput().getText()).isEqualTo("It is sunny in Tokyo.");
		assertThat(toolArguments).containsExactly("{\"city\":\"Tokyo\"}");
		// Same shape as the log of the real ChatClient in Lab 01.
		assertThat(this.log).containsExactly("outsideLoop:before", "insideLoop:before", "insideLoop:after",
				"insideLoop:before", "insideLoop:after", "outsideLoop:after");
		assertThat(model.receivedPrompts().get(1).getInstructions()).extracting(m -> m.getMessageType())
			.containsExactly(MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL);
	}

}
