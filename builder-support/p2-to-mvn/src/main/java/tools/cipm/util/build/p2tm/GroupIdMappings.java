package tools.cipm.util.build.p2tm;

import java.util.HashMap;
import java.util.Map;

public final class GroupIdMappings {
    public static final Map<String, String> ARTIFACT_ID_TO_GROUP_ID = new HashMap<>();

    static {
        ARTIFACT_ID_TO_GROUP_ID.put("org.xtext.lua", "tools.cipm");
		ARTIFACT_ID_TO_GROUP_ID.put("tools.cipm.models.instrumentation", "tools.cipm");
		ARTIFACT_ID_TO_GROUP_ID.put("jamopp.parser.jdt", "tools.mdsd");
		ARTIFACT_ID_TO_GROUP_ID.put("org.palladiosimulator.pcm", "org.palladiosimulator.core");
		ARTIFACT_ID_TO_GROUP_ID.put("de.uka.ipd.sdq.identifier", "org.palladiosimulator.commons");
		ARTIFACT_ID_TO_GROUP_ID.put("de.uka.ipd.sdq.stoex", "org.palladiosimulator.commons");
		ARTIFACT_ID_TO_GROUP_ID.put("de.uka.ipd.sdq.probfunction", "org.palladiosimulator.commons");
		ARTIFACT_ID_TO_GROUP_ID.put("de.uka.ipd.sdq.units", "org.palladiosimulator.commons");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.uml2.uml", "org.eclipse.uml2");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.uml2.common", "org.eclipse.uml2");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.uml2.types", "org.eclipse.uml2");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.uml2.uml.resources", "org.eclipse.uml2");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.uml2.uml.profile.standard", "org.eclipse.uml2");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.ocl", "org.eclipse.ocl");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.ocl.ecore", "org.eclipse.ocl");
		ARTIFACT_ID_TO_GROUP_ID.put("org.eclipse.ocl.common", "org.eclipse.ocl");
		ARTIFACT_ID_TO_GROUP_ID.put("aopalliance", "aopalliance");
    }

    private GroupIdMappings() {}
}
