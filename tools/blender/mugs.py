"""
Meowcha Café : génération des tasses (Blender 3.6+ / 4.x).
Coller dans l'onglet Scripting puis Run Script.
Chaque tasse est un objet séparé, espacé sur l'axe X.
EXPORT_DIR : si renseigné, exporte chaque tasse en .glb (pour app/src/main/assets/models).
"""
import bpy, bmesh, math, os

EXPORT_DIR = ""  # ex. r"D:\dev\mobile\meowcha\app\src\main\assets\models"

# nom, couleur corps, couleur intérieur, oreilles de chat, soucoupe
MUGS = [
    ("mug_classic", (0.97, 0.93, 0.86), (0.55, 0.35, 0.22), False, True),
    ("mug_matcha",  (0.62, 0.78, 0.55), (0.45, 0.62, 0.30), True,  True),
    ("mug_sakura",  (0.98, 0.72, 0.80), (0.55, 0.35, 0.22), True,  False),
    ("mug_minuit",  (0.20, 0.22, 0.38), (0.30, 0.20, 0.14), True,  False),
]

def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()

def mat(name, rgb, rough=0.35):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*rgb, 1)
    b.inputs["Roughness"].default_value = rough
    return m

def lathe(name, profile, segments=48):
    """Révolution d'un profil (r, z) autour de Z."""
    mesh = bpy.data.meshes.new(name)
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    bm = bmesh.new()
    rings = []
    for r, z in profile:
        ring = []
        for i in range(segments):
            a = 2 * math.pi * i / segments
            ring.append(bm.verts.new((r * math.cos(a), r * math.sin(a), z)))
        rings.append(ring)
    for k in range(len(rings) - 1):
        for i in range(segments):
            j = (i + 1) % segments
            bm.faces.new((rings[k][i], rings[k][j], rings[k + 1][j], rings[k + 1][i]))
    # fermer le fond (centre)
    c = bm.verts.new((0, 0, profile[0][1]))
    for i in range(segments):
        bm.faces.new((c, rings[0][(i + 1) % segments], rings[0][i]))
    c2 = bm.verts.new((0, 0, profile[-1][1]))
    for i in range(segments):
        bm.faces.new((c2, rings[-1][i], rings[-1][(i + 1) % segments]))
    bm.normal_update()
    bm.to_mesh(mesh); bm.free()
    for p in mesh.polygons: p.use_smooth = True
    return obj

def body(name):
    # extérieur vers le haut, lèvre, puis intérieur vers le bas (paroi épaisse, look toy)
    prof = [
        (0.30, 0.00), (0.34, 0.02), (0.36, 0.10), (0.38, 0.40),
        (0.40, 0.70), (0.41, 0.78), (0.39, 0.80),           # lèvre arrondie
        (0.35, 0.78), (0.34, 0.50), (0.32, 0.20), (0.26, 0.10),
    ]
    return lathe(name, prof)

def coffee(name):
    return lathe(name, [(0.335, 0.60), (0.335, 0.62)])

def saucer(name):
    return lathe(name, [(0.25, -0.04), (0.45, -0.03), (0.60, 0.02), (0.62, 0.05), (0.58, 0.04), (0.30, 0.00)])

def handle(name):
    bpy.ops.mesh.primitive_torus_add(major_radius=0.17, minor_radius=0.05,
                                     major_segments=32, minor_segments=12,
                                     location=(0.42, 0, 0.42), rotation=(math.pi / 2, 0, 0))
    h = bpy.context.active_object; h.name = name
    h.scale = (0.85, 1, 1.15)
    bpy.ops.object.shade_smooth()
    return h

def ear(name, x_sign):
    bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.10, depth=0.14,
                                    location=(0, x_sign * 0.28, 0.85))
    e = bpy.context.active_object; e.name = name
    e.rotation_euler = (x_sign * -0.35, 0, math.pi / 4)
    bev = e.modifiers.new("bevel", "BEVEL"); bev.width = 0.02; bev.segments = 3
    bpy.ops.object.shade_smooth()
    return e

def build(idx, name, col, inner, ears, with_saucer):
    parts = []
    b = body(name); parts.append(b)
    b.data.materials.append(mat(f"{name}_body", col))
    c = coffee(f"{name}_drink"); c.data.materials.append(mat(f"{name}_drink", inner, 0.15)); parts.append(c)
    h = handle(f"{name}_handle"); h.data.materials.append(b.data.materials[0]); parts.append(h)
    if ears:
        for s in (-1, 1):
            e = ear(f"{name}_ear", s); e.data.materials.append(b.data.materials[0]); parts.append(e)
    if with_saucer:
        s = saucer(f"{name}_saucer"); s.data.materials.append(mat("saucer_white", (0.98, 0.97, 0.95))); parts.append(s)
    # fusion en un seul objet
    bpy.ops.object.select_all(action="DESELECT")
    for p in parts: p.select_set(True)
    bpy.context.view_layer.objects.active = b
    bpy.ops.object.join()
    b.name = name
    sub = b.modifiers.new("subsurf", "SUBSURF"); sub.levels = 1; sub.render_levels = 2
    b.location.x = idx * 1.6
    return b

clear_scene()
objs = [build(i, *m) for i, m in enumerate(MUGS)]

if EXPORT_DIR:
    os.makedirs(EXPORT_DIR, exist_ok=True)
    for o in objs:
        loc = o.location.copy(); o.location = (0, 0, 0)
        bpy.ops.object.select_all(action="DESELECT"); o.select_set(True)
        bpy.ops.export_scene.gltf(filepath=os.path.join(EXPORT_DIR, o.name + ".glb"),
                                  use_selection=True, export_apply=True)
        o.location = loc
print("Tasses générées :", [o.name for o in objs])
