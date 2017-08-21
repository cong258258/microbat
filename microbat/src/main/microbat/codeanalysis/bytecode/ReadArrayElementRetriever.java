package microbat.codeanalysis.bytecode;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;

/**
 * 
 * 
 * @author "linyun"
 *
 */
public class ReadArrayElementRetriever extends ASTNodeRetriever{
	String typeName;
	List<String> arrayElementNameList = new ArrayList<>();
	
	public ReadArrayElementRetriever(CompilationUnit cu, int lineNumber, String typeName){
		super(cu, lineNumber, "");
		this.typeName = typeName;
	}
	
	private Expression retrieveName(ArrayAccess access){
		Expression arrayExp = access.getArray();
		while(arrayExp instanceof ArrayAccess){
			ArrayAccess childAccess = (ArrayAccess)arrayExp;
			arrayExp = childAccess.getArray();
		}
		
		return arrayExp;
	}
	
	public boolean visit(ArrayAccess access){
		int linNum = cu.getLineNumber(access.getStartPosition());
		if(linNum == lineNumber){
			Expression arrayExp = retrieveName(access);
			if(arrayExp instanceof Name){
				Name name = (Name)arrayExp;
				ITypeBinding typeBinding = name.resolveTypeBinding();
				if(typeBinding != null){
					if(typeBinding.isArray()){
						String arrayType = typeBinding.getElementType().getName();
						if(arrayType.equals(typeName)){
							String arrayElementName = access.toString();
							if(!arrayElementNameList.contains(arrayElementName)){
								arrayElementNameList.add(arrayElementName);							
							}
							return false;
						}
					}
				}
				//In this case, we are not parsing eclipse project so the binding is not available.
				else{
					String arrayElementName = access.toString();
					if(!arrayElementNameList.contains(arrayElementName)){
						arrayElementNameList.add(arrayElementName);							
					}
					return true;
				}
				
			}
		}
		return true;
	}
	
}