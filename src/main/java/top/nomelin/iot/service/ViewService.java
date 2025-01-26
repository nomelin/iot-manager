package top.nomelin.iot.service;

import top.nomelin.iot.model.View;

import java.util.List;

/**
 * ViewService
 *
 * @author nomelin
 * @since 2025/01/26 23:30
 **/
public interface ViewService {
    View checkPermission(int viewId);

    View addView(View view, int groupId);

    void deleteView(int viewId);

    View updateView(View view);

    View getViewById(int viewId);

    List<View> getViewsByIds(List<Integer> viewIds);

    List<View> getViewsByGroupId(int groupId);
}
