package rs.readahead.washington.mobile.domain.repository.uwazi;

import java.util.List;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate;

public interface ICollectUwaziTemplatesRepository {
     Single<List<CollectTemplate>> listBlankTemplates();
    Single<List<CollectTemplate>> listFavoriteTemplates();
    Single<CollectTemplate> updateBlankTemplates(List<CollectTemplate> templates);
}
