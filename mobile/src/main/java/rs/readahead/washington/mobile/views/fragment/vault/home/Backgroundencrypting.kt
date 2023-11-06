package rs.readahead.washington.mobile.views.fragment.vault.home

import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityModel
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityStatus
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityType

object  BackgroundencryptingMock {

     fun generateMockDataList(): List<BackgroundActivityModel> {
        val dataList = mutableListOf<BackgroundActivityModel>()

        // Add mock data items to the list
        dataList.add(BackgroundActivityModel("1", "File 1", BackgroundActivityType.FILE, "image/png", null, BackgroundActivityStatus.IN_PROGRESS))
        dataList.add(BackgroundActivityModel("2", "File 2", BackgroundActivityType.FILE, "audio/mpeg", null, BackgroundActivityStatus.COMPLETED))
        dataList.add(BackgroundActivityModel("3", "Other Activity 1", BackgroundActivityType.OTHER, "text/plain", null, BackgroundActivityStatus.FAILED))
        dataList.add(BackgroundActivityModel("4", "File 3", BackgroundActivityType.FILE, "video/mp4", null, BackgroundActivityStatus.IN_PROGRESS))

        // Add more items as needed for testing various scenarios

        return dataList
    }
}