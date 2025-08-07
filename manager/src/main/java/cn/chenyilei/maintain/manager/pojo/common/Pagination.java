//package cn.chenyilei.maintain.manager.pojo.common;
//
//import com.github.pagehelper.Page;
//import com.google.common.collect.Lists;
//import lombok.Data;
//
//import java.io.Serializable;
//import java.util.Collections;
//import java.util.List;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
/// **
// * 分页、list传递对象 , 只有当使用{@link com.github.pagehelper.PageHelper}分页时才会对page对象生效
// * 没有list 也没生成空list
// *
// * @author chenyilei
// * @date 2022/08/23 19:10
// */
//@Data
//@SuppressWarnings({"unchecked", "rawtypes"})
//public class Pagination<T> implements Serializable {
//
//    private static final long serialVersionUID = 1L;
//    //当前页
//    private int pageNum;
//    //每页的数量
//    private int pageSize;
//
//    //当前页的数量
//    private int size;
//    //排序
//    private String orderBy;
//
//    //总记录数
//    private int total;
//    //总页数
//    private int pages;
//    //结果集
//    private List<T> list;
//
//    /**
//     * 包装Page对象
//     *
//     * @param list
//     */
//    public Pagination(List<T> list) {
//        this(list, 8);
//    }
//
//    /**
//     * 包装Page对象
//     *
//     * @param list          page结果
//     * @param navigatePages 页码数量
//     */
//    public Pagination(List<T> list, int navigatePages) {
//        if (list instanceof Page) {
//            Page page = (Page) list;
//            this.pageNum = page.getPageNum();
//            this.pageSize = page.getPageSize();
//            this.orderBy = page.getOrderBy();
//
//            this.pages = page.getPages();
//            this.list = page;
//            this.size = page.size();
//            this.total = (int) page.getTotal();
//        } else if (list != null) {
//            this.pageNum = 1;
//            this.pageSize = list.size();
//
//            this.pages = 1;
//            this.list = list;
//            this.size = list.size();
//            this.total = list.size();
//        } else if (list == null) {
//            this.list = Lists.newArrayList();
//            this.pageNum = 1;
//            this.pageSize = 0;
//
//            this.pages = 1;
//            this.size = 0;
//            this.total = 0;
//        }
//    }
//
//    public List<T> getList() {
//        return this.list;
//    }
//
//    public static <T> Pagination<T> of(List<T> list, Long total) {
//        Pagination<T> tPagination = new Pagination<>(list);
//        if (null != total) {
//            tPagination.setTotal(total.intValue());
//        }
//        return tPagination;
//    }
//
//    public static <T> Pagination<T> of(List<T> list, int total) {
//        Pagination<T> tPagination = new Pagination<>(list);
//        tPagination.setTotal(total);
//        return tPagination;
//    }
//
//    public static <T> Pagination<T> empty() {
//        return Pagination.of(null);
//    }
//
//    public static <T> Pagination<T> emptyList() {
//        return Pagination.of(Lists.newArrayList(), 0);
//    }
//
//    public boolean isEmpty() {
//        return this.list == null || this.list.size() == 0;
//    }
//
//    public static <T> Pagination<T> of(List<T> list) {
//        return new Pagination<>(list);
//    }
//
//    //转移分页参数
//    public static <T> Pagination<T> of(List<T> list, Pagination pagination) {
//        if (pagination != null) {
//            Pagination<T> result = new Pagination<>(list);
//            result.setPageNum(pagination.getPageNum());
//            result.setPageSize(pagination.getPageSize());
//            result.setSize(pagination.getSize());
//            result.setOrderBy(pagination.getOrderBy());
//            result.setTotal(pagination.getTotal());
//            result.setPages(pagination.getPages());
//            return result;
//        } else {
//            return new Pagination<>(list);
//        }
//    }
//
//    /**
//     * @param list           转换后list
//     * @param paginationList 用pagehelper后的原始对象list
//     * @param <T>
//     * @return
//     */
//    public static <T> Pagination<T> of(List<T> list, List paginationList) {
//        if (paginationList instanceof Page) {
//            Page pagination = (Page) paginationList;
//
//            Pagination<T> result = new Pagination<>(list);
//            result.setPageNum(pagination.getPageNum());
//            result.setPageSize(pagination.getPageSize());
//            result.setSize(pagination.size());
//            result.setOrderBy(pagination.getOrderBy());
//            result.setTotal((int) pagination.getTotal());
//            result.setPages(pagination.getPages());
//            return result;
//        } else {
//            return new Pagination<>(list);
//        }
//    }
//
//    //转移分页参数
//    public static <T> Pagination<T> ofNoNew(List<T> list, Pagination pagination) {
//        if (pagination != null) {
//            pagination.setList(list);
//            return pagination;
//        } else {
//            return new Pagination<>(list);
//        }
//    }
//
//
//    public <R> List<R> mapCollectList(Class<R> rClass, Function<T, R> mapFunction) {
//        if (isEmpty()) {
//            return Collections.emptyList();
//        }
//        return this.list.stream().map(mapFunction).collect(Collectors.toList());
//    }
//
//    public T one() {
//        if (isEmpty()) {
//            return null;
//        }
//        return this.list.get(0);
//    }
//
//}
