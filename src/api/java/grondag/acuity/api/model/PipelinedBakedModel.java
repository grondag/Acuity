package grondag.acuity.api.model;

import java.util.Iterator;

public interface PipelinedBakedModel
{
    Iterator<PipelinedBakedQuad> getPipelinedQuads(BlockModelInputs inputAccess);
}


