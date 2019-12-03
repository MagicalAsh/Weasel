package us.magicalash.weasel.provider.representation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PullResponse extends ProviderResponse {
    public List<ProvidedRepository> files;
}
