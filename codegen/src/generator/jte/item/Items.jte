@import java.util.List
@import pirated.codegen.item.ParsedItem
@import pirated.codegen.item.RenderUtil

@param List<ParsedItem> items
package pirated.items.generated

import pirated.items.ItemDef

/** Generated, do not edit by hand. The KDoc on an entry is the wire ids it answers to. */
object Items {
@for(ParsedItem item : items)
${RenderUtil.INSTANCE.declaration(item)}
@endfor

  private fun item(name: String, price: Int) = ItemDef(name, price)
}
