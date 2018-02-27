package microbat.instrumentation.runtime;

import java.util.Stack;

import microbat.model.trace.TraceNode;

public class MethodCallStack extends Stack<TraceNode> {
	private static final long serialVersionUID = 1L;

	public TraceNode safePop() {
		if (size() != 0) {
			return pop();
		}
		return null;
	}

	public TraceNode push(TraceNode node) {
		return super.push(node);
	}
	
	@Override
	public synchronized TraceNode peek() {
		if (isEmpty()) {
			return null;
		}
		return super.peek();
	}

	/**
	 * return whether we need to change the invocation layer structure by exception
	 * @param methodSignature
	 * @return
	 */
	public boolean popForException(String methodSignature) {
		if(!this.isEmpty()){
			int popLayer = 0;
			boolean needPop = false;
			for(int i=this.size()-1; i>=0; i--){
				TraceNode caller = this.get(i);
				popLayer++;
				if(caller.getMethodSign().equals(methodSignature)){
					needPop = true;
				}
			}
			
			if(needPop){
				for(int i=0; i<popLayer; i++){
					this.pop();
				}
				
				return true;
			}
		}
		
		return false;
	}
	
}
