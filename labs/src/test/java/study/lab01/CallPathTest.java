package study.lab01;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 01: follow one request through the real {@link ChatClient}, with a scripted model
 * instead of a provider.
 */
class CallPathTest {

	record WeatherRequest(String city) {
	}

	@Test
	void advisorsWrapTheModelCallInOrderOfTheirOrderValue() {
		List<String> log = new ArrayList<>();
		ScriptedChatModel model = new ScriptedChatModel(new AssistantMessage("hello"));

		ChatClient chatClient = ChatClient.builder(model)
			// Registered in the "wrong" order on purpose: getOrder() decides, not this list.
			.defaultAdvisors(new RecordingAdvisor("inner", 10, log), new RecordingAdvisor("outer", 0, log))
			.build();

		String content = chatClient.prompt().user("hi").call().content();

		assertThat(content).isEqualTo("hello");
		assertThat(log).containsExactly("outer:before", "inner:before", "inner:after", "outer:after");
		assertThat(model.receivedPrompts()).hasSize(1);
	}

	@Test
	void toolCallMakesTheAdvisorLoopCallTheModelTwice() {
		List<String> log = new ArrayList<>();
		List<String> citiesAskedFor = new ArrayList<>();

		ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", (WeatherRequest request) -> {
			citiesAskedFor.add(request.city());
			return "sunny";
		}).description("Get the current weather for a city").inputType(WeatherRequest.class).build();

		ScriptedChatModel model = new ScriptedChatModel(
				// Round 1: the "model" asks for a tool instead of answering.
				AssistantMessage.builder()
					.content("")
					.toolCalls(List.of(new ToolCall("call-1", "function", "get_weather", "{\"city\":\"Tokyo\"}")))
					.build(),
				// Round 2: with the tool result in the prompt, it answers.
				new AssistantMessage("It is sunny in Tokyo."));

		ChatClient chatClient = ChatClient.builder(model)
			.defaultAdvisors(
					// Lower order than the tool calling advisor => outside its loop.
					new RecordingAdvisor("outsideLoop", ToolCallingAdvisor.DEFAULT_ORDER - 1, log),
					// Higher order => inside the loop, so it runs once per round.
					new RecordingAdvisor("insideLoop", ToolCallingAdvisor.DEFAULT_ORDER + 1, log))
			.build();

		String content = chatClient.prompt().user("Weather in Tokyo?").toolCallbacks(weatherTool).call().content();

		assertThat(content).isEqualTo("It is sunny in Tokyo.");
		assertThat(citiesAskedFor).containsExactly("Tokyo");

		// Nobody registered a ToolCallingAdvisor: ChatClient added one on its own, and it
		// re-ran the rest of the chain after executing the tool.
		assertThat(log).containsExactly("outsideLoop:before", "insideLoop:before", "insideLoop:after",
				"insideLoop:before", "insideLoop:after", "outsideLoop:after");

		List<Prompt> prompts = model.receivedPrompts();
		assertThat(prompts).hasSize(2);
		assertThat(prompts.get(0).getInstructions()).extracting(m -> m.getMessageType())
			.containsExactly(MessageType.USER);
		// The second prompt is the first one plus the tool round trip.
		assertThat(prompts.get(1).getInstructions()).extracting(m -> m.getMessageType())
			.containsExactly(MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL);
	}

}
