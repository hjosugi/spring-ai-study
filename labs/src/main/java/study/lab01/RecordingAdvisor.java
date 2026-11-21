package study.lab01;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

/**
 * A {@link CallAdvisor} that does nothing except write "name:before" and "name:after"
 * into a shared log, so a test can see where in the chain it ran and how many times.
 */
public final class RecordingAdvisor implements CallAdvisor {

	private final String name;

	private final int order;

	private final List<String> log;

	public RecordingAdvisor(String name, int order, List<String> log) {
		this.name = name;
		this.order = order;
		this.log = log;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		this.log.add(this.name + ":before");
		ChatClientResponse response = chain.nextCall(request);
		this.log.add(this.name + ":after");
		return response;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
