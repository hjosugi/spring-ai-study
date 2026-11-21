package study.lab02;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The idea behind Spring AI's {@code DefaultAroundAdvisorChain}, rebuilt small enough to
 * read in one sitting. It reuses the real {@link Prompt}, {@link ChatResponse} and
 * {@link ChatModel} types and leaves out ordering by {@code getOrder()}, streaming,
 * observations and the request/response context map.
 *
 * <p>
 * Two things carry over from the real class:
 * <ul>
 * <li>The model call is just the last advisor in the list (the real one is
 * {@code ChatModelCallAdvisor}).</li>
 * <li>The chain is consumed as it runs: {@link #next(Prompt)} pops the next advisor off
 * a deque. An advisor that wants to run the rest of the chain more than once therefore
 * needs {@link #copyAfter(MiniAdvisor)}, which is what {@code CallAdvisorChain.copy}
 * is for.</li>
 * </ul>
 */
public final class MiniChain {

	@FunctionalInterface
	public interface MiniAdvisor {

		/**
		 * Do something before, call {@code chain.next(prompt)} (or not), do something
		 * after.
		 */
		ChatResponse advise(Prompt prompt, MiniChain chain);

	}

	/** Every advisor of this chain, outermost first. Never modified. */
	private final List<MiniAdvisor> all;

	/** The advisors that have not run yet. */
	private final Deque<MiniAdvisor> remaining;

	private MiniChain(List<MiniAdvisor> advisors) {
		this.all = List.copyOf(advisors);
		this.remaining = new ArrayDeque<>(advisors);
	}

	/**
	 * @param advisorsOutermostFirst the first advisor sees the request first and the
	 * response last
	 */
	public static MiniChain of(ChatModel model, List<MiniAdvisor> advisorsOutermostFirst) {
		List<MiniAdvisor> advisors = new ArrayList<>(advisorsOutermostFirst);
		advisors.add((prompt, chain) -> model.call(prompt));
		return new MiniChain(advisors);
	}

	public ChatResponse next(Prompt prompt) {
		if (this.remaining.isEmpty()) {
			throw new IllegalStateException("No advisors left to run");
		}
		return this.remaining.pop().advise(prompt, this);
	}

	/** A fresh chain made of the advisors that come after the given one. */
	public MiniChain copyAfter(MiniAdvisor advisor) {
		int index = this.all.indexOf(advisor);
		if (index < 0) {
			throw new IllegalArgumentException("Advisor is not part of this chain");
		}
		return new MiniChain(this.all.subList(index + 1, this.all.size()));
	}

}
