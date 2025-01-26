package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.ViewMapper;
import top.nomelin.iot.model.View;
import top.nomelin.iot.service.GroupService;
import top.nomelin.iot.service.ViewService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewServiceImpl
 *
 * @author nomelin
 * @since 2025/01/26 23:42
 **/
@Service
public class ViewServiceImpl implements ViewService {
    private static final Logger log = LoggerFactory.getLogger(ViewServiceImpl.class);
    private final ViewMapper viewMapper;
    private final GroupService groupService;

    public ViewServiceImpl(ViewMapper viewMapper, GroupService groupService, CurrentUserCache currentUserCache) {
        this.viewMapper = viewMapper;
        this.groupService = groupService;
    }

    @Override
    public View checkPermission(int viewId) {
        View view = viewMapper.selectById(viewId);
        if (view == null) {
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }
        groupService.checkPermission(view.getGroupId());
        return view;
    }

    @Override
    public View addView(View view, int groupId) {
        // 1. 校验组权限
        groupService.checkPermission(groupId);
        // 2. 校验物理量有效性
        validateMeasurement(groupId, view.getMeasurement());
//        // 3. 校验视图唯一性。目前认为可以对同一组内对同一物理量创建多个视图,所以不需要
//        validateViewUniqueness(groupId, view.getMeasurement());
        // 4. 设置组ID并保存
        view.setGroupId(groupId);
        viewMapper.insert(view);
        log.info("视图创建成功：{}", view);
        return view;
    }

    @Override
    public void deleteView(int viewId) {
        checkPermission(viewId);
        viewMapper.delete(viewId);
        log.info("视图删除成功：ID={}", viewId);
    }

    @Override
    public View updateView(View view) {
        View existing = checkPermission(view.getId());

        // 如果修改了物理量需要重新校验
        if (!existing.getMeasurement().equals(view.getMeasurement())) {
            validateMeasurement(existing.getGroupId(), view.getMeasurement());
//            validateViewUniqueness(existing.getGroupId(), view.getMeasurement());
        }

        viewMapper.update(view);
        log.info("视图更新成功：{}", view);
        return viewMapper.selectById(view.getId());
    }

    @Override
    public View getViewById(int viewId) {
        return checkPermission(viewId);
    }

    @Override
    public List<View> getViewsByIds(List<Integer> viewIds) {
        return viewIds.stream()
                .map(this::checkPermission)
                .collect(Collectors.toList());
    }

    @Override
    public List<View> getViewsByGroupId(int groupId) {
        groupService.checkPermission(groupId);
        return viewMapper.selectByGroupId(groupId);
    }


//    /**
//     * 校验视图唯一性, 同一组内不能对同一物理量创建多个视图
//     */
//    private View validateViewUniqueness(int groupId, String measurement) {
//        View existing = viewMapper.selectByGroupIdAndMeasurement(groupId, measurement);
//        if (existing != null) {
//            throw new BusinessException(CodeMessage.DUPLICATE_ERROR);
//        }
//        return existing;
//    }

    /**
     * 校验物理量有效性, 物理量必须在组内有效
     */
    private void validateMeasurement(int groupId, String measurement) {
        List<String> validMeasurements = groupService.getAllMeasurement(groupId);
        if (!validMeasurements.contains(measurement)) {
            throw new BusinessException(CodeMessage.PARAM_ERROR);
        }
    }
}
