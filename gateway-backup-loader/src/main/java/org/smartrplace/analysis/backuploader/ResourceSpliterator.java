package org.smartrplace.analysis.backuploader;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;

/**
 *
 * @author jlapp
 */
public class ResourceSpliterator implements Spliterator<Resource> {

	Deque<IterationState> stack = new ArrayDeque<>();
	Set<Resource> visitedOnPath = new HashSet<>();
	int maxDepth;
	boolean followReferences = true;

	static class IterationState {

		Resource res;
		List<Resource> children;
		int childIndex;

		public IterationState(Resource res, List<Resource> children, int childIndex) {
			if (!(res instanceof ResourceList)) {
				Collections.sort(children, (r1, r2) -> r1.getName().compareTo(r2.getName()));
			}
			this.res = res;
			this.children = children;
			this.childIndex = childIndex;
		}

	}

	/*
		public ResourceSpliterator(Resource start) {
			this(start, Integer.MAX_VALUE);
		}
	 */
	public ResourceSpliterator(Resource start, int maxDepth, boolean followReferences) {
		if (maxDepth < 0) {
			throw new IllegalArgumentException("maxDepth must be >= 0");
		}
		stack.push(new IterationState(start, getNextChildren(start, followReferences), 0));
		this.maxDepth = maxDepth;
		this.followReferences = followReferences;
	}

	// false => post order
	final boolean preorder = true;

	protected static List<Resource> getNextChildren(Resource p, boolean followReferences) {
		return followReferences
				? p.getSubResources(false)
				: p.getDirectSubResources(false);
	}

	@Override
	public boolean tryAdvance(Consumer<? super Resource> action) {
		while (!stack.isEmpty()) {
			IterationState it = stack.peek();
			if (preorder && it.childIndex == 0) { //preorder
				action.accept(it.res);
			}
			//System.out.printf("current: %d/%d, %s%n", it.childIndex, it.children.size(), it.res.getPath());
			if (it.children.isEmpty()) {
				// only used when the starting resource is empty, otherwise
				// leaf resources are returned directly
				if (!preorder) {
					action.accept(it.res);
				}
				stack.pop();
				visitedOnPath.remove(it.res.getLocationResource());
				return true;
			}
			if (it.childIndex == it.children.size()) {
				// return intermediate node after all children have been processed
				if (!preorder) {
					action.accept(it.res);
				}
				stack.pop();
				visitedOnPath.remove(it.res.getLocationResource());
				return true;
			}
			Resource next = it.children.get(it.childIndex++);
			if (stack.size() - 1 < maxDepth && !visitedOnPath.contains(next.getLocationResource())) {
				List<Resource> nextChildren = getNextChildren(next, followReferences);
				if (nextChildren.isEmpty()) { // return leaf node directly
					action.accept(next);
					return true;
				} else {
					visitedOnPath.add(next.getLocationResource());
					stack.push(new IterationState(next, nextChildren, 0));
				}
			} // else: max depth reached or looping path, do not descend further
		}
		return false;
	}

	@Override
	public Spliterator<Resource> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public int characteristics() {
		return Spliterator.ORDERED | Spliterator.CONCURRENT;
	}

}
