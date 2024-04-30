package software.xdev.tci.portfixation;

import java.util.Set;


/**
 * Attach this interface when {@link PortFixation} is required and the exposed ports are not known during
 * instantiation.
 */
public interface AdditionalPortsForFixedExposingContainer
{
	Set<Integer> getAdditionalPortsForFixedExposing();
}
