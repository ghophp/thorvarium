import filters.CORSFilter
import play.api.GlobalSettings
import play.api.mvc.WithFilters

object Global extends WithFilters(new CORSFilter) with GlobalSettings
